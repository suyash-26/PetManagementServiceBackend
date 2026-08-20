package com.example.petManagementService.pet.service;

import com.example.petManagementService.CareCenter.entities.CareCenter;
import com.example.petManagementService.CareCenter.enums.CenterStatus;
import com.example.petManagementService.CareCenter.repository.CareCenterRepository;
import com.example.petManagementService.pet.dto.AdoptionListingCreateRequest;
import com.example.petManagementService.pet.dto.AdoptionListingResponse;
import com.example.petManagementService.pet.entity.AdoptionListing;
import com.example.petManagementService.pet.entity.Pet;
import com.example.petManagementService.pet.enums.ListingStatus;
import com.example.petManagementService.pet.enums.PetStatus;
import com.example.petManagementService.pet.enums.Species;
import com.example.petManagementService.pet.mapper.AdoptionListingMapper;
import com.example.petManagementService.pet.repository.AdoptionListingRepository;
import com.example.petManagementService.pet.repository.PetRepository;
import com.example.petManagementService.requests.services.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

// Flow C — a center lists a pet it holds. There is deliberately NO user-facing creation
// path: that omission, plus the custody invariant below, is the entire enforcement of v2's
// "a pet can never move directly from one user to another".
@Service
@RequiredArgsConstructor
public class AdoptionListingService {

    private final AdoptionListingRepository listingRepository;
    private final PetRepository petRepository;
    private final CareCenterRepository careCenterRepository;
    private final AdoptionListingMapper listingMapper;
    private final CustodyService custodyService;
    private final RequestService requestService;

    @Transactional
    public AdoptionListingResponse createListing(UUID centerId, AdoptionListingCreateRequest dto,
                                                 Long adminUserId, String callerRole) {
        // Authorization: a member of THIS center, or SUPER_ADMIN. Same check the request
        // engine and the intake queue use.
        requestService.requireCenterAccess(centerId, adminUserId, callerRole);

        Pet pet = petRepository.findById(dto.petId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PET_NOT_FOUND"));

        // THE INVARIANT (v2 §5): a listing may only be created when the pet is in THIS
        // center's custody and is IN_CENTER_CUSTODY. Both halves matter — the first stops
        // Center B listing Center A's animal, the second stops a boarding guest or an
        // already-listed pet being listed. This single check is what makes peer-to-peer
        // adoption structurally impossible.
        if (!centerId.equals(pet.getCustodianCenterId()) || pet.getStatus() != PetStatus.IN_CENTER_CUSTODY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "PET_NOT_IN_YOUR_CUSTODY: pet is " + pet.getStatus()
                            + " and held by " + pet.getCustodianCenterId());
        }

        AdoptionListing listing = new AdoptionListing();
        listing.setPet(pet);
        listing.setCenterId(centerId);
        listing.setListedByAdminId(adminUserId);
        listing.setDescription(dto.description());
        listing.setReason(dto.reason());
        listing.setAdoptionFee(dto.adoptionFee());
        listing.setListingStatus(ListingStatus.OPEN);
        AdoptionListing saved = listingRepository.save(listing);

        // Same transaction, so the listing and the pet's visibility flip together. Note
        // this reloads the pet by id — inside one transaction JPA returns the SAME managed
        // instance from its persistence context, so there's no second SELECT and no risk
        // of two divergent copies.
        custodyService.markAvailableForAdoption(pet.getId());

        return listingMapper.toResponse(saved);
    }

    // The public feed. Anonymous — no principal, no center scoping, OPEN listings only.
    @Transactional(readOnly = true)
    public List<AdoptionListingResponse> getFeed(Species species, String city) {
        List<AdoptionListing> listings;

        if (city != null && !city.isBlank()) {
            List<UUID> centerIds = careCenterRepository
                    .findByStatusAndCityIgnoreCase(CenterStatus.ACTIVE, city)
                    .stream().map(CareCenter::getId).toList();
            // Early return: an "IN ()" against an empty collection is either a SQL error or
            // a silently-always-false predicate depending on the dialect. Answer it here.
            if (centerIds.isEmpty()) {
                return List.of();
            }
            listings = species != null
                    ? listingRepository.findByListingStatusAndPet_SpeciesAndCenterIdIn(
                    ListingStatus.OPEN, species, centerIds)
                    : listingRepository.findByListingStatusAndCenterIdIn(ListingStatus.OPEN, centerIds);
        } else {
            listings = species != null
                    ? listingRepository.findByListingStatusAndPet_Species(ListingStatus.OPEN, species)
                    : listingRepository.findByListingStatus(ListingStatus.OPEN);
        }

        return listings.stream().map(listingMapper::toResponse).toList();
    }

    // The center's own listing list — admin-scoped, every status. Deliberately separate
    // from getFeed(): that one is anonymous and hardcoded to OPEN, so an admin browsing it
    // would never see the listings that actually need attention.
    @Transactional(readOnly = true)
    public List<AdoptionListingResponse> getCenterListings(UUID centerId, ListingStatus status,
                                                           Long callerId, String callerRole) {
        requestService.requireCenterAccess(centerId, callerId, callerRole);

        List<AdoptionListing> listings = status != null
                ? listingRepository.findByCenterIdAndListingStatusOrderByPostedAtDesc(centerId, status)
                : listingRepository.findByCenterIdOrderByPostedAtDesc(centerId);

        return listings.stream().map(listingMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AdoptionListingResponse getById(UUID listingId) {
        return listingMapper.toResponse(listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LISTING_NOT_FOUND")));
    }

    // Delist (v2 §8's PATCH /listings/{id}). Only from OPEN: a RESERVED listing has an
    // approved adopter mid-handover, and that adoption request must be cancelled through
    // the request engine first so its own status and history stay truthful.
    @Transactional
    public AdoptionListingResponse delist(UUID listingId, Long adminUserId, String callerRole) {
        AdoptionListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LISTING_NOT_FOUND"));

        requestService.requireCenterAccess(listing.getCenterId(), adminUserId, callerRole);

        if (listing.getListingStatus() != ListingStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "LISTING_NOT_OPEN: cannot delist a " + listing.getListingStatus() + " listing");
        }

        listing.setListingStatus(ListingStatus.CLOSED);
        custodyService.revertToInCenterCustody(listing.getPet().getId());

        return listingMapper.toResponse(listingRepository.save(listing));
    }
}