package com.example.petManagementService.pet.dto;

import com.example.petManagementService.pet.enums.Gender;
import com.example.petManagementService.pet.enums.ListingStatus;
import com.example.petManagementService.pet.enums.Species;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// Flattens the pet's public-facing fields into the listing, so the feed renders from one
// payload without a second call per card. Deliberately omits ownerUserId,
// custodianCenterId and surrenderedByUserId — this is an anonymous public response, and
// who surrendered an animal is not the adopter's business.
public record AdoptionListingResponse(
        UUID id,
        UUID petId,
        String petName,
        Species species,
        String breed,
        Gender gender,
        LocalDate dateOfBirth,
        String size,
        String color,
        Boolean vaccinated,
        Boolean sterilized,
        UUID centerId,
        String description,
        String reason,
        BigDecimal adoptionFee,
        ListingStatus listingStatus,
        Instant postedAt
) {
}