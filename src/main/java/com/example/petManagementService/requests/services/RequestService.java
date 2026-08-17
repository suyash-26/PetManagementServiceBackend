package com.example.petManagementService.requests.services;

import com.example.petManagementService.CareCenter.repository.CenterMemberRepository;
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
        // GAP: for an INTAKE request this should also flip the pet to PENDING_INTAKE.
        // Needs the pet module (out of scope for this pass).
        Request saved = requestRepository.save(request);
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
        // GAP: for an INTAKE request whose pet already moved to PENDING_INTAKE, cancelling
        // should revert the pet to OWNED. Needs the pet module.
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
        // GAP: this is where the actual custody transfer belongs — pets.custodian_center_id,
        // pets.owner_user_id, pets.status, plus a pet_custody_history row, all in this same
        // transaction (CustodyService.transfer() per the design doc). Not implemented here;
        // needs the pet module. Completing today only finalizes the request record itself.
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
