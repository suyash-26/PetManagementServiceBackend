package com.example.petManagementService.BoardingRequests.dto;

import java.time.LocalDate;
import java.util.UUID;

// v2 §6.3's occupancy calculation: overlapping approved boardings + pets physically held
// in custody, against care_centers.capacity.
public record CenterAvailabilityResponse(
        UUID centerId,
        LocalDate from,
        LocalDate to,
        int capacity,
        int occupiedByBoardings,
        int occupiedByCustody,
        int available
) {
}