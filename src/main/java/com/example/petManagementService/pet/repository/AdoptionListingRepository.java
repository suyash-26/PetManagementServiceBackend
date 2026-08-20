package com.example.petManagementService.pet.repository;

import com.example.petManagementService.pet.entity.AdoptionListing;
import com.example.petManagementService.pet.enums.ListingStatus;
import com.example.petManagementService.pet.enums.Species;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AdoptionListingRepository extends JpaRepository<AdoptionListing, UUID> {

    // The public feed. Four derived methods rather than one @Query with nullable params:
    // "(:centerIds is null or l.centerId in :centerIds)" is a known Hibernate trap, and an
    // if/else ladder in the service mirrors CareCenterService.search() and
    // RequestService.getCenterRequests(), which already handle optional filters this way.
    List<AdoptionListing> findByListingStatus(ListingStatus status);

    // Pet_Species with the underscore: species is a column on pets, not on
    // adoption_listings, so Spring Data hops through the pet association to reach it.
    List<AdoptionListing> findByListingStatusAndPet_Species(ListingStatus status, Species species);

    // centerId is a plain UUID column here, NOT a @ManyToOne — so JPQL cannot join to
    // care_centers to filter on city. The service resolves matching centers first and
    // passes their ids in.
    List<AdoptionListing> findByListingStatusAndCenterIdIn(
            ListingStatus status, Collection<UUID> centerIds);

    List<AdoptionListing> findByListingStatusAndPet_SpeciesAndCenterIdIn(
            ListingStatus status, Species species, Collection<UUID> centerIds);

    // The admin's own listings. Unlike the public feed above these are NOT filtered to
    // OPEN — RESERVED and CLOSED are exactly the ones a center needs to see, since a
    // RESERVED listing has an approved adopter mid-handover. Newest first.
    List<AdoptionListing> findByCenterIdOrderByPostedAtDesc(UUID centerId);

    List<AdoptionListing> findByCenterIdAndListingStatusOrderByPostedAtDesc(
            UUID centerId, ListingStatus listingStatus);
}