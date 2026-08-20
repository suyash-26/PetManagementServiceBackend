package com.example.petManagementService.requests.services;

import com.example.petManagementService.CareCenter.repository.CenterMemberRepository;
import com.example.petManagementService.pet.service.CustodyService;
import com.example.petManagementService.requests.dto.RequestResponse;
import com.example.petManagementService.requests.dto.RequestStatusHistoryResponse;
import com.example.petManagementService.requests.entities.Request;
import com.example.petManagementService.requests.entities.RequestStatusHistory;
import com.example.petManagementService.requests.enums.RequestStatus;
import com.example.petManagementService.requests.enums.RequestType;
import com.example.petManagementService.requests.mapper.RequestMapper;
import com.example.petManagementService.requests.repositories.RequestRepository;
import com.example.petManagementService.requests.repositories.RequestStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.petManagementService.AdoptionRequests.entities.AdoptionRequest;
import com.example.petManagementService.AdoptionRequests.repository.AdoptionRequestRepository;
import com.example.petManagementService.pet.entity.AdoptionListing;
import com.example.petManagementService.pet.enums.ListingStatus;
import com.example.petManagementService.pet.repository.AdoptionListingRepository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// The shared approval state machine: PENDING -> APPROVED -> COMPLETED, with
// PENDING -> REJECTED and PENDING/APPROVED -> CANCELLED branches. One engine for every
// request type (INTAKE/ADOPTION/BOARDING/GENERAL) per the design doc's "polymorphic
// Request sharing a single approval state-machine" idea.
//
// Deliberately generic here: approving/completing an INTAKE request should also move
// the underlying pet (e.g. -> PENDING_INTAKE, then custody transfer + a
// pet_custody_history row on complete), but that requires the pet module, which is out
// of scope for this pass. Those are marked as explicit gaps below rather than silently
// skipped — wire them in once pet/custody work lands.
@Service
@RequiredArgsConstructor
public class RequestService {

    // "no other active request" guards elsewhere (e.g. intake) key off this same
    // definition of "still in flight" — PENDING or APPROVED, not yet at a terminal state.
    public static final Set<RequestStatus> ACTIVE_STATUSES = Set.of(RequestStatus.PENDING, RequestStatus.APPROVED);

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final RequestRepository requestRepository;
    private final RequestStatusHistoryRepository requestStatusHistoryRepository;
    private final CenterMemberRepository centerMemberRepository;
    private final RequestMapper requestMapper;
    private final CustodyService custodyService;
    private final AdoptionRequestRepository adoptionRequestRepository;
    private final AdoptionListingRepository adoptionListingRepository;

    @Transactional(readOnly = true)
    public List<RequestResponse> getMyRequests(Long requesterUserId) {
        return requestRepository.findByRequesterUserId(requesterUserId).stream()
                .map(requestMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> getCenterRequests(UUID centerId, RequestStatus status, RequestType type,
                                                    Long callerId, String callerRole) {
        requireCenterAccess(centerId, callerId, callerRole);

        List<Request> requests;
        if (status != null && type != null) {
            requests = requestRepository.findByCareCenter_IdAndStatusAndRequestType(centerId, status, type);
        } else if (status != null) {
            requests = requestRepository.findByCareCenter_IdAndStatus(centerId, status);
        } else if (type != null) {
            requests = requestRepository.findByCareCenter_IdAndRequestType(centerId, type);
        } else {
            requests = requestRepository.findByCareCenter_Id(centerId);
        }
        return requests.stream().map(requestMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RequestResponse getById(UUID requestId) {
        return requestMapper.toResponse(findOrThrow(requestId));
    }

    // "the decider is now always a CENTER_ADMIN of request.center_id" — now actually
    // enforced via requireCenterAccess() (CenterMember lookup), not just the role-level
    // @PreAuthorize on the controller. SUPER_ADMIN bypasses per-center scoping —
    // platform owner authority per the doc's role table.
    @Transactional
    public RequestResponse approve(UUID requestId, Long adminUserId, String callerRole) {
        Request request = findOrThrow(requestId);
        requireCenterAccess(request.getCareCenter().getId(), adminUserId, callerRole);
        RequestStatus previousStatus = requireStatus(request, RequestStatus.PENDING, RequestStatus.APPROVED);

        request.setStatus(RequestStatus.APPROVED);
        request.setAssignedAdmin(adminUserId);
        request.setDecidedAt(Instant.now());
        // Intake approval is a promise: the pet is earmarked for handover but stays with
        // its owner until complete(). ADOPTION's equivalent (listing → RESERVED, pet →
        // RESERVED) lands with the listing module; BOARDING moves the pet at check-in, not
        // approval; GENERAL has no pet at all.
        switch (request.getRequestType()) {
            // Intake approval earmarks the pet; it stays with its owner until complete().
            case INTAKE -> custodyService.markPendingIntake(request.getPetId(), request.getId());
            // Adoption approval locks the listing to one applicant; nothing transfers yet.
            case ADOPTION -> applyAdoptionApproval(request);
            // BOARDING moves the pet at check-in, not approval. GENERAL has no pet at all.
            default -> { }
        }        Request saved = requestRepository.save(request);
        recordStatusChange(saved, previousStatus, RequestStatus.APPROVED, adminUserId, null);
        return requestMapper.toResponse(saved);
    }

    @Transactional
    public RequestResponse reject(UUID requestId, Long adminUserId, String callerRole, String remark) {
        Request request = findOrThrow(requestId);
        requireCenterAccess(request.getCareCenter().getId(), adminUserId, callerRole);
        RequestStatus previousStatus = requireStatus(request, RequestStatus.PENDING, RequestStatus.REJECTED);

        request.setStatus(RequestStatus.REJECTED);
        request.setAssignedAdmin(adminUserId);
        request.setDecidedAt(Instant.now());
        if (remark != null && !remark.isBlank()) {
            request.setNotes(remark);
        }
        Request saved = requestRepository.save(request);
        recordStatusChange(saved, previousStatus, RequestStatus.REJECTED, adminUserId, remark);
        return requestMapper.toResponse(saved);
    }

    // Deliberately NOT role-gated at the controller: "PENDING/APPROVED -> CANCELLED
    // (requester or admin)". The requester always can; a CENTER_ADMIN can only if
    // they're actually a member of *this* request's center (or SUPER_ADMIN, which
    // bypasses center scoping entirely).
    @Transactional
    public RequestResponse cancel(UUID requestId, Long callerUserId, String callerRole) {
        Request request = findOrThrow(requestId);
        if (request.getStatus() != RequestStatus.PENDING && request.getStatus() != RequestStatus.APPROVED) {
            throw illegalTransition(request.getStatus(), RequestStatus.CANCELLED);
        }

        boolean isRequester = request.getRequesterUserId() != null
                && request.getRequesterUserId().equals(callerUserId);
        boolean isCenterAdmin = SUPER_ADMIN.equals(callerRole)
                || centerMemberRepository.existsByCenter_IdAndUserId(request.getCareCenter().getId(), callerUserId);
        if (!isRequester && !isCenterAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NOT_REQUESTER_OR_ADMIN");
        }

        RequestStatus previousStatus = request.getStatus();
        request.setStatus(RequestStatus.CANCELLED);
        request.setDecidedAt(Instant.now());
        // Only an APPROVED intake ever moved the pet. Cancelling a still-PENDING one must
        // not touch it — the pet never left OWNED, and revertPendingIntake() would throw
        // ILLEGAL_CUSTODY_TRANSITION on a pet that's already OWNED.
        // Only an APPROVED request ever moved anything; a still-PENDING one has nothing to
        // undo, and the revert methods would throw ILLEGAL_CUSTODY_TRANSITION.
        if (previousStatus == RequestStatus.APPROVED) {
            switch (request.getRequestType()) {
                case INTAKE -> custodyService.revertPendingIntake(request.getPetId(), request.getId());
                case ADOPTION -> applyAdoptionCancellation(request);
                default -> { }
            }
        }
        Request saved = requestRepository.save(request);
        recordStatusChange(saved, previousStatus, RequestStatus.CANCELLED, callerUserId, null);
        return requestMapper.toResponse(saved);
    }

    @Transactional
    public RequestResponse complete(UUID requestId, Long adminUserId, String callerRole) {
        Request request = findOrThrow(requestId);
        requireCenterAccess(request.getCareCenter().getId(), adminUserId, callerRole);
        RequestStatus previousStatus = requireStatus(request, RequestStatus.APPROVED, RequestStatus.COMPLETED);

        request.setStatus(RequestStatus.COMPLETED);
        request.setDecidedAt(Instant.now());
        // Custody transfer for INTAKE only — an ADOPTION/BOARDING "complete" doesn't mean
        // the same thing (adoption moves a pet already IN_CENTER_CUSTODY to its adopter;
        // boarding is a loan, not a transfer) and neither module is implemented yet, so
        // this stays scoped rather than guessing their semantics.
        switch (request.getRequestType()) {
            case INTAKE -> custodyService.transferToCenter(
                    request.getPetId(), request.getCareCenter().getId(), request.getId(), adminUserId);
            case ADOPTION -> applyAdoptionCompletion(request, adminUserId);
            // BOARDING's real effects are check-in/check-out. GENERAL has no custody effect.
            default -> { }
        }
        Request saved = requestRepository.save(request);
        recordStatusChange(saved, previousStatus, RequestStatus.COMPLETED, adminUserId, null);
        return requestMapper.toResponse(saved);
    }

    // Real audit trail now (requests.entities.RequestStatusHistory) — one row per
    // transition, oldest first, including the initial PENDING row IntakeRequestService
    // (or any future request-creating service) records via recordInitialCreation().
    @Transactional(readOnly = true)
    public List<RequestStatusHistoryResponse> getHistory(UUID requestId) {
        findOrThrow(requestId); // 404 if the request itself doesn't exist
        return requestStatusHistoryRepository.findByRequest_IdOrderByChangedAtAsc(requestId).stream()
                .map(requestMapper::toHistoryResponse)
                .toList();
    }

    // Public: called by IntakeRequestService (and, later, adoption/boarding) right
    // after a new Request is first saved as PENDING, so the audit trail starts at
    // creation instead of at the first approve/reject/cancel/complete.
    @Transactional
    public void recordInitialCreation(Request request, Long requesterUserId) {
        recordStatusChange(request, null, RequestStatus.PENDING, requesterUserId, null);
    }

    // Public: the center-scoping check other request-creating services (IntakeRequestService's
    // admin queue today) need too — kept here rather than duplicated, since it's the same
    // CenterMember lookup + SUPER_ADMIN bypass regardless of which module is asking.
    public void requireCenterAccess(UUID centerId, Long userId, String callerRole) {
        if (SUPER_ADMIN.equals(callerRole)) {
            return; // platform owner overrides per-center scoping
        }
        if (!centerMemberRepository.existsByCenter_IdAndUserId(centerId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NOT_CENTER_ADMIN: not an admin of this center");
        }
    }

    Request findOrThrow(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND"));
    }

    private RequestStatus requireStatus(Request request, RequestStatus expectedCurrent, RequestStatus target) {
        if (request.getStatus() != expectedCurrent) {
            throw illegalTransition(request.getStatus(), target);
        }
        return expectedCurrent;
    }

    private ResponseStatusException illegalTransition(RequestStatus from, RequestStatus to) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "ILLEGAL_TRANSITION: " + from + " -> " + to);
    }

    // Flow D step 2 — approve one applicant. The listing goes RESERVED so no one else can
    // apply or be approved, and the pet goes RESERVED so it leaves the public feed.
    // Nothing transfers: the animal is still at the center.
    private void applyAdoptionApproval(Request request) {
        AdoptionListing listing = listingFor(request);

        // The race guard the doc asks for: two admins approving two different applicants
        // for the same listing. The second one loses here rather than double-reserving.
        if (listing.getListingStatus() != ListingStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "ALREADY_RESERVED: this listing already has an approved adopter");
        }

        listing.setListingStatus(ListingStatus.RESERVED);
        adoptionListingRepository.save(listing);
        custodyService.markReserved(request.getPetId());
    }

    // Flow D step 3 — the handover, all in the caller's single transaction: ownership
    // moves, the listing closes, and every rival application is auto-rejected with a
    // reason. This is the ONLY point at which an adopter becomes the legal owner.
    private void applyAdoptionCompletion(Request request, Long adminUserId) {
        AdoptionRequest detail = adoptionDetailFor(request);
        AdoptionListing listing = listingFor(request);

        custodyService.transferToAdopter(request.getPetId(), detail.getAdopterUserId(),
                request.getId(), adminUserId);

        listing.setListingStatus(ListingStatus.CLOSED);
        adoptionListingRepository.save(listing);

        for (AdoptionRequest rival : adoptionRequestRepository
                .findByListingIdAndRequest_StatusIn(listing.getId(), ACTIVE_STATUSES)) {
            Request rivalRequest = rival.getRequest();
            if (rivalRequest.getId().equals(request.getId())) {
                continue; // skip the winner, which this method is completing
            }
            RequestStatus from = rivalRequest.getStatus();
            rivalRequest.setStatus(RequestStatus.REJECTED);
            rivalRequest.setAssignedAdmin(adminUserId);
            rivalRequest.setDecidedAt(Instant.now());
            requestRepository.save(rivalRequest);
            // Rivals get their own history rows — a rejected applicant deserves an audit
            // trail explaining why, same as any admin-initiated rejection.
            recordStatusChange(rivalRequest, from, RequestStatus.REJECTED, adminUserId,
                    "AUTO_REJECTED: another applicant was approved for this listing");
        }
    }

    // Flow D step 4 — approved adopter never collected. The listing reopens and the pet
    // goes back on the shelf, so other applicants can still be considered.
    private void applyAdoptionCancellation(Request request) {
        AdoptionListing listing = listingFor(request);
        listing.setListingStatus(ListingStatus.OPEN);
        adoptionListingRepository.save(listing);
        custodyService.revertToAvailableForAdoption(request.getPetId());
    }

    private AdoptionRequest adoptionDetailFor(Request request) {
        // 500, not 404: an ADOPTION request without its detail row is corrupt data, not a
        // bad client call — the two are written together in one transaction.
        return adoptionRequestRepository.findById(request.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "ADOPTION_DETAIL_MISSING for request " + request.getId()));
    }

    private AdoptionListing listingFor(Request request) {
        return adoptionListingRepository.findById(adoptionDetailFor(request).getListingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "LISTING_MISSING for adoption request " + request.getId()));
    }

    private void recordStatusChange(Request request, RequestStatus from, RequestStatus to, Long changedBy, String notes) {
        RequestStatusHistory history = new RequestStatusHistory();
        history.setRequest(request);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(changedBy);
        history.setNotes(notes);
        requestStatusHistoryRepository.save(history);
    }
}
