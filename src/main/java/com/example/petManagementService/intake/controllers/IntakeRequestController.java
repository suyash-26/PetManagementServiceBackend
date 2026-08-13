package com.example.petManagementService.intake.controllers;

import com.example.petManagementService.common.security.AuthenticatedUser;
import com.example.petManagementService.intake.dto.IntakeRequestCreateRequest;
import com.example.petManagementService.intake.dto.IntakeRequestResponse;
import com.example.petManagementService.intake.services.IntakeRequestService;
import com.example.petManagementService.requests.enums.RequestStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

// Flow B — hand a pet to a center. Approve/reject/cancel/complete on the resulting
// Request are handled generically by RequestController — this controller only covers
// creation and the center's own intake queue.
@RestController
@RequestMapping("/intake")
@RequiredArgsConstructor
public class IntakeRequestController {

    private final IntakeRequestService intakeRequestService;

    // Raised by the pet's owner, not an admin — no role restriction.
    @PostMapping
    public ResponseEntity<IntakeRequestResponse> addIntakeRequest(
            @Valid @RequestBody IntakeRequestCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        IntakeRequestResponse response = intakeRequestService.createIntake(currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Admin review queue for a center's intake requests.
    // TODO: once CenterMember lookup is wired in, scope to admins of *this* center id.
    @PreAuthorize("hasRole('CENTER_ADMIN')")
    @GetMapping("/centers/{id}/intake-requests")
    public ResponseEntity<List<IntakeRequestResponse>> getCentersIntakeRequest(
            @PathVariable UUID id,
            @RequestParam(required = false) RequestStatus status) {
        return ResponseEntity.ok(intakeRequestService.getCenterIntakeRequests(id, status));
    }

    // GAP: custody roster (which pets a center currently holds) needs the pet module —
    // out of scope for this pass. Left unimplemented rather than faked.
    @PreAuthorize("hasRole('CENTER_ADMIN')")
    @GetMapping("/centers/{centreId}/custody")
    public ResponseEntity<String> getAllPetsInCustodyForGivenCentre(@PathVariable UUID centreId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("Custody roster requires the pet module (out of scope) - not yet implemented.");
    }
}
