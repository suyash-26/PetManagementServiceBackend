package com.example.petManagementService.requests.controllers;

import com.example.petManagementService.common.security.AuthenticatedUser;
import com.example.petManagementService.requests.dto.RequestActionRequest;
import com.example.petManagementService.requests.dto.RequestResponse;
import com.example.petManagementService.requests.enums.RequestStatus;
import com.example.petManagementService.requests.enums.RequestType;
import com.example.petManagementService.requests.services.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private static final String CENTER_ADMIN = "CENTER_ADMIN";
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final RequestService requestService;

    @GetMapping("/mine")
    public ResponseEntity<List<RequestResponse>> getMyAllRequests(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(requestService.getMyRequests(currentUser.id()));
    }

    // Admin queue for a center. Role-gated only (CENTER_ADMIN) — not yet scoped to
    // "admin of *this* center specifically", which needs CenterMember (out of scope).
    @PreAuthorize("hasRole('" + CENTER_ADMIN + "')")
    @GetMapping("/centers/{id}/requests")
    public ResponseEntity<List<RequestResponse>> getAllRequestAgainstThisCenter(
            @PathVariable UUID id,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) RequestType type) {
        return ResponseEntity.ok(requestService.getCenterRequests(id, status, type));
    }

    // "the decider is now always a CENTER_ADMIN of request.center_id" — v2's
    // simplification of v1's dual-approver (owner-or-admin) branch.
    @PreAuthorize("hasRole('" + CENTER_ADMIN + "')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<RequestResponse> approveRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(requestService.approve(id, currentUser.id()));
    }

    @PreAuthorize("hasRole('" + CENTER_ADMIN + "')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<RequestResponse> rejectRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestBody(required = false) RequestActionRequest body) {
        String remark = body != null ? body.notes() : null;
        return ResponseEntity.ok(requestService.reject(id, currentUser.id(), remark));
    }

    // Deliberately NOT @PreAuthorize-restricted: "requester or admin" per the doc.
    @PostMapping("/{id}/cancel")
    public ResponseEntity<RequestResponse> cancelRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        boolean callerIsCenterAdmin = CENTER_ADMIN.equals(currentUser.role()) || SUPER_ADMIN.equals(currentUser.role());
        return ResponseEntity.ok(requestService.cancel(id, currentUser.id(), callerIsCenterAdmin));
    }

    // Where custody/ownership would actually transfer for INTAKE/ADOPTION — see the GAP
    // note in RequestService.complete(); today this only finalizes the request record.
    @PreAuthorize("hasRole('" + CENTER_ADMIN + "')")
    @PostMapping("/{id}/complete")
    public ResponseEntity<RequestResponse> completeRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(requestService.complete(id, currentUser.id()));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<RequestResponse>> getRequestHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(requestService.getHistory(id));
    }
}
