package com.example.petManagementService.requests.dto;

import com.example.petManagementService.requests.enums.RequestStatus;

import java.time.Instant;
import java.util.UUID;

public record RequestStatusHistoryResponse(
        UUID id,
        RequestStatus fromStatus,
        RequestStatus toStatus,
        Long changedBy,
        Instant changedAt,
        String notes
) {
}
