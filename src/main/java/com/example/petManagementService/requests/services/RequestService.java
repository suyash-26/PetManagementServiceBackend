package com.example.petManagementService.requests.services;

import com.example.petManagementService.requests.dto.RequestResponse;
import com.example.petManagementService.requests.entities.Request;
import com.example.petManagementService.requests.enums.RequestStatus;
import com.example.petManagementService.requests.enums.RequestType;
import com.example.petManagementService.requests.mapper.RequestMapper;
import com.example.petManagementService.requests.repositories.RequestRepository;
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

    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;

    @Transactional(readOnly = true)
    public List<RequestResponse> getMyRequests(Long requesterUserId) {
        return requestRepository.findByRequesterUserId(requesterUserId).stream()
                .map(requestMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> getCenterRequests(UUID centerId, RequestStatus status, RequestType type) {
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

    // "the decider is now always a CENTER_ADMIN of request.center_id" — the role check
    // (@PreAuthorize("hasRole('CENTER_ADMIN')")) lives on the controller; scoping to
    // *this specific* center's admins needs CenterMember, which is out of scope here, so
    // today any CENTER_ADMIN can decide any center's request. Tracked as a gap, not
    // silently done "correctly".
    @Transactional
    public RequestResponse approve(UUID requestId, Long adminUserId) {
        Request request = findOrThrow(requestId);
        requireStatus(request, RequestStatus.PENDING, RequestStatus.APPROVED);

        request.setStatus(RequestStatus.APPROVED);
        request.setAssignedAdmin(adminUserId);
        request.setDecidedAt(Instant.now());
        // GAP: for an INTAKE request this should also flip the pet to PENDING_INTAKE.
        // Needs the pet module (out of scope for this pass).
        return requestMapper.toResponse(requestRepository.save(request));
    }

    @Transactional
    public RequestResponse reject(UUID requestId, Long adminUserId, String remark) {
        Request request = findOrThrow(requestId);
        requireStatus(request, RequestStatus.PENDING, RequestStatus.REJECTED);

        request.setStatus(RequestStatus.REJECTED);
        request.setAssignedAdmin(adminUserId);
        request.setDecidedAt(Instant.now());
        if (remark != null && !remark.isBlank()) {
            request.setNotes(remark);
        }
        return requestMapper.toResponse(requestRepository.save(request));
    }

    // Deliberately NOT admin-only: "PENDING/APPROVED -> CANCELLED (requester or admin)".
    // callerIsCenterAdmin is role-level only (see the gap note on approve()) — it doesn't
    // verify the caller administers *this* request's center specifically.
    @Transactional
    public RequestResponse cancel(UUID requestId, Long callerUserId, boolean callerIsCenterAdmin) {
        Request request = findOrThrow(requestId);
        if (request.getStatus() != RequestStatus.PENDING && request.getStatus() != RequestStatus.APPROVED) {
            throw illegalTransition(request.getStatus(), RequestStatus.CANCELLED);
        }

        boolean isRequester = request.getRequesterUserId() != null
                && request.getRequesterUserId().equals(callerUserId);
        if (!isRequester && !callerIsCenterAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NOT_REQUESTER_OR_ADMIN");
        }

        request.setStatus(RequestStatus.CANCELLED);
        request.setDecidedAt(Instant.now());
        // GAP: for an INTAKE request whose pet already moved to PENDING_INTAKE, cancelling
        // should revert the pet to OWNED. Needs the pet module.
        return requestMapper.toResponse(requestRepository.save(request));
    }

    @Transactional
    public RequestResponse complete(UUID requestId, Long adminUserId) {
        Request request = findOrThrow(requestId);
        requireStatus(request, RequestStatus.APPROVED, RequestStatus.COMPLETED);

        request.setStatus(RequestStatus.COMPLETED);
        request.setDecidedAt(Instant.now());
        // GAP: this is where the actual custody transfer belongs — pets.custodian_center_id,
        // pets.owner_user_id, pets.status, plus a pet_custody_history row, all in this same
        // transaction (CustodyService.transfer() per the design doc). Not implemented here;
        // needs the pet module. Completing today only finalizes the request record itself.
        return requestMapper.toResponse(requestRepository.save(request));
    }

    // No dedicated request_status_history audit table exists yet (the doc's "Immutable
    // audit trail of every status transition" entity) — this returns the request's
    // current snapshot as a single-entry history until that table/entity is built.
    @Transactional(readOnly = true)
    public List<RequestResponse> getHistory(UUID requestId) {
        return List.of(requestMapper.toResponse(findOrThrow(requestId)));
    }

    Request findOrThrow(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND"));
    }

    private void requireStatus(Request request, RequestStatus expectedCurrent, RequestStatus target) {
        if (request.getStatus() != expectedCurrent) {
            throw illegalTransition(request.getStatus(), target);
        }
    }

    private ResponseStatusException illegalTransition(RequestStatus from, RequestStatus to) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "ILLEGAL_TRANSITION: " + from + " -> " + to);
    }
}
