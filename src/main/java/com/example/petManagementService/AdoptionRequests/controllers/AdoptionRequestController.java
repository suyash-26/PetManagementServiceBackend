package com.example.petManagementService.AdoptionRequests.controllers;

import com.example.petManagementService.AdoptionRequests.dto.AdoptionRequestCreateRequest;
import com.example.petManagementService.AdoptionRequests.dto.AdoptionRequestResponse;
import com.example.petManagementService.AdoptionRequests.service.AdoptionRequestService;
import com.example.petManagementService.common.security.AuthenticatedUser;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Flow D — adopter applies to a listing. Approve/reject/cancel/complete on the
// resulting Request are handled generically by RequestController — this controller
// only covers creation and the admin's applicant queue for one listing.
@RestController
@RequestMapping("/listings")
@RequiredArgsConstructor
public class AdoptionRequestController {

    private final AdoptionRequestService adoptionRequestService;

    // Raised by the adopter, not an admin — no role restriction, mirrors addIntakeRequest.
    @PostMapping("/{id}/requests")
    public ResponseEntity<AdoptionRequestResponse> addAdoptionRequest(
            @PathVariable UUID id,
            @Valid @RequestBody AdoptionRequestCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        AdoptionRequestResponse response = adoptionRequestService.applyToListing(currentUser.id(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Admin's applicant queue for a listing (Flow D step 2: "admin sees BOTH applicants
    // in one queue").
    // TODO: once CenterMember lookup is wired in, scope to admins of *this* listing's center.
    @PreAuthorize("hasRole('CENTER_ADMIN')")
    @GetMapping("/{id}/applicants")
    public ResponseEntity<List<AdoptionRequestResponse>> getApplicants(@PathVariable UUID id) {
        return ResponseEntity.ok(adoptionRequestService.getApplicants(id));
    }
}
