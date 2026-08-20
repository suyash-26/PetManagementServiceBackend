package com.example.petManagementService.pet.mapper;

import com.example.petManagementService.pet.dto.AdoptionListingResponse;
import com.example.petManagementService.pet.entity.AdoptionListing;
import com.example.petManagementService.pet.entity.Pet;
import org.springframework.stereotype.Component;

@Component
public class AdoptionListingMapper {

    // listing.getPet() is a LAZY association, so every call site must be inside an open
    // transaction — every method in AdoptionListingService is @Transactional for exactly
    // this reason. Mapping outside one throws LazyInitializationException.
    public AdoptionListingResponse toResponse(AdoptionListing listing) {
        Pet pet = listing.getPet();
        return new AdoptionListingResponse(
                listing.getId(),
                pet.getId(),
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getGender(),
                pet.getDateOfBirth(),
                pet.getSize(),
                pet.getColor(),
                pet.getVaccinated(),
                pet.getSterilized(),
                listing.getCenterId(),
                listing.getDescription(),
                listing.getReason(),
                listing.getAdoptionFee(),
                listing.getListingStatus(),
                listing.getPostedAt());
    }
}