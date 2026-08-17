package com.example.petManagementService.requests.controllers;

import com.example.petManagementService.common.security.AuthenticatedUser;
import com.example.petManagementService.requests.dto.RequestActionRequest;
import com.example.petManagementService.requests.dto.RequestResponse;
import com.example.petManagementService.requests.dto.RequestStatusHistoryResponse;
import com.example.petManagementService.requests.enums.RequestStatus;
import com.example.petManagementService.requests.enums.RequestType;
import com.example.petManagementService.requests.services.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// The shared admin queue + approval-engine endpoints — one set of endpoints for every
// request type (INTAKE/ADOPTION/BOARDING/GENERAL), per the design doc's polymorphic
// Request sharing a single approval state-machine.
@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class RequestController {

    // NOT role-gated at the controller, deliberately: per v2 §3, center-admin-ness comes
    // from holding a center_members row, not from the JWT `role` claim (CareCenterController
    // follows the same rule — update()/addMember()/listMembers() have no @PreAuthorize
    // either, just CenterGuard). An earlier hasAnyRole('CENTER_ADMIN','SUPER_ADMIN') gate
    // here silently blocked every center OWNER/STAFF who was never separately promoted to
    // the CENTER_ADMIN role claim — which is most of them, since nothing else in the app
    // grants that claim. RequestService.requireCenterAccess() (CenterMember lookup, with a
    // SUPER_ADMIN bypass) is the real and sufficient authorization check below.
    private final RequestService requestService;

    @GetMapping("/mine")
    public ResponseEntity<List<RequestResponse>> getMyAllRequests(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(requestService.getMyRequests(currentUser.id()));
    }

    // Admin queue for a center — scoped to admins of *this* center (or SUPER_ADMIN) via
    // RequestService.requireCenterAccess().
    @GetMapping("/centers/{id}/requests")
    public ResponseEntity<List<RequestResponse>> getAllRequestAgainstThisCenter(
            @PathVariable UUID id,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) RequestType type,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(requestService.getCenterRequests(id, status, type, currentUser.id(), currentUser.role()));
    }

    // "the decider is now always a CENTER_ADMIN of request.center_id" — v2's
    // simplification of v1's dual-approver (owner-or-admin) branch.
    @PostMapping("/{id}/approve")
    public ResponseEntity<RequestResponse> approveRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(requestService.approve(id, currentUser.id(), currentUser.role()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<RequestResponse> rejectRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestBody(required = false) RequestActionRequest body) {
        String remark = body != null ? body.notes() : null;
        return ResponseEntity.ok(requestService.reject(id, currentUser.id(), currentUser.role(), remark));
    }

    // Deliberately NOT @PreAuthorize-restricted: "requester or admin" per the doc —
    // RequestService.cancel() does the requester-or-center-admin-or-SUPER_ADMIN check.
    @PostMapping("/{id}/cancel")
    public ResponseEntity<RequestResponse> cancelRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(requestService.cancel(id, currentUser.id(), currentUser.role()));
    }

    // Where custody/ownership would actually transfer for INTAKE/ADOPTION — see the GAP
    // note in RequestService.complete(); today this only finalizes the request record.
    @PostMapping("/{id}/complete")
    public ResponseEntity<RequestResponse> completeRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(requestService.complete(id, currentUser.id(), currentUser.role()));
    }

    // Real chronological audit trail now — see RequestStatusHistory.
    @GetMapping("/{id}/history")
    public ResponseEntity<List<RequestStatusHistoryResponse>> getRequestHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(requestService.getHistory(id));
    }
}
