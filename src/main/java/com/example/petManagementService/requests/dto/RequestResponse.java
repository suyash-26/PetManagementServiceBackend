package com.example.petManagementService.requests.dto;

import com.example.petManagementService.CareCenter.entities.CareCenter;
import com.example.petManagementService.requests.enums.RequestStatus;
import com.example.petManagementService.requests.enums.RequestType;

import java.time.Instant;
import java.util.UUID;

public record RequestResponse(
        UUID id,
        RequestType requestType,
        Long requesterUserId,
        UUID petId,
        CareCenter careCenter,
        RequestStatus status,
        Long assignedAdmin,
        String notes,
        Instant createdAt,
        Instant decidedAt
) {
}
