package com.example.petManagementService.pet.controller;

import com.example.petManagementService.common.security.AuthenticatedUser;
import com.example.petManagementService.pet.dto.AdoptionListingCreateRequest;
import com.example.petManagementService.pet.dto.AdoptionListingResponse;
import com.example.petManagementService.pet.enums.ListingStatus;
import com.example.petManagementService.pet.enums.Species;
import com.example.petManagementService.pet.service.AdoptionListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// No class-level @RequestMapping: creation hangs off /centers/{id}/listings (the resource
// belongs to a center) while reads hang off /listings (a public catalogue), exactly as
// v2 §8 specifies. AdoptionRequestController also maps /listings for /{id}/requests and
// /{id}/applicants — no clash, the sub-paths differ.
@RestController
@RequiredArgsConstructor
public class AdoptionListingController {

    private final AdoptionListingService listingService;

    // Admin-only by construction: there is no user-facing equivalent of this endpoint, and
    // that absence is the enforcement of the center-mediated rule.
    @PostMapping("/centers/{centerId}/listings")
    public ResponseEntity<AdoptionListingResponse> create(
            @PathVariable UUID centerId,
            @Valid @RequestBody AdoptionListingCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                listingService.createListing(centerId, request, currentUser.id(), currentUser.role()));
    }

    // Admin-scoped counterpart to the public feed below. Same path as create(), different
    // method — which is why a missing GET here answered 405 rather than 404.
    @GetMapping("/centers/{centerId}/listings")
    public ResponseEntity<List<AdoptionListingResponse>> centerListings(
            @PathVariable UUID centerId,
            @RequestParam(required = false) ListingStatus status,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(listingService.getCenterListings(
                centerId, status, currentUser.id(), currentUser.role()));
    }

    // Public feed — no @AuthenticationPrincipal, so it works for anonymous browsers.
    @GetMapping("/listings")
    public ResponseEntity<List<AdoptionListingResponse>> feed(
            @RequestParam(required = false) Species species,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(listingService.getFeed(species, city));
    }

    @GetMapping("/listings/{id}")
    public ResponseEntity<AdoptionListingResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(listingService.getById(id));
    }

    @PatchMapping("/listings/{id}")
    public ResponseEntity<AdoptionListingResponse> delist(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(listingService.delist(id, currentUser.id(), currentUser.role()));
    }
}