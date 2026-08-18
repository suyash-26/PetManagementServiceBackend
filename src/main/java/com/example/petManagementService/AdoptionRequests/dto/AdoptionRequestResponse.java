package com.example.petManagementService.AdoptionRequests.dto;

import com.example.petManagementService.CareCenter.entities.CareCenter;
import com.example.petManagementService.requests.enums.RequestStatus;

import java.time.Instant;
import java.util.UUID;

// careCenter matches IntakeRequestResponse's shape (full entity, not a flat centerId) —
// kept consistent across response DTOs.
public record AdoptionRequestResponse(
        UUID requestId,
        UUID listingId,
        UUID petId,
        CareCenter careCenter,
        RequestStatus status,
        Long adopterUserId,
        String message,
        String screeningAnswers,
        Boolean homeVisitStatus,
        Instant createdAt,
        Instant decidedAt
) {
}
