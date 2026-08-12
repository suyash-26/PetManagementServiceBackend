package com.example.petManagementService.intake.dto;

import com.example.petManagementService.CareCenter.entities.CareCenter;
import com.example.petManagementService.intake.enums.CustodyMode;
import com.example.petManagementService.intake.enums.IntakeReason;
import com.example.petManagementService.requests.enums.RequestStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// careCenter matches RequestResponse's shape (full entity, not a flat centerId) —
// kept consistent across both response DTOs.
public record IntakeRequestResponse(
        UUID requestId,
        UUID petId,
        CareCenter careCenter,
        RequestStatus status,
        Long requesterUserId,
        IntakeReason reason,
        CustodyMode custodyMode,
        LocalDate handoverDate,
        String ownerNotes,
        String vetRecordsUrl,
        Instant handoverCompletedAt,
        Instant createdAt,
        Instant decidedAt
) {
}
