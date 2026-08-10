package com.example.petManagementService.BoardingRequests.dto;

import com.example.petManagementService.requests.enums.RequestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BoardingRequestResponse(
        UUID requestId,
        UUID petId,
        UUID centerId,
        UUID requesterUserId,
        RequestStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String specialInstructions,
        BigDecimal quotedPrice,
        Instant checkedInAt,
        Instant checkedOutAt
) {}
