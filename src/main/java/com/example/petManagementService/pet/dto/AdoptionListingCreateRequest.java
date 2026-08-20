package com.example.petManagementService.pet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

// centerId is not here — it comes from the path (/centers/{centerId}/listings), same as
// AdoptionRequestCreateRequest takes its listingId from the path. listingStatus and
// listedByAdminId are server-derived and must never be client-supplied.
public record AdoptionListingCreateRequest(
        @NotNull UUID petId,
        @Size(max = 4000) String description,
        @Size(max = 2000) String reason,
        @PositiveOrZero BigDecimal adoptionFee
) {
}