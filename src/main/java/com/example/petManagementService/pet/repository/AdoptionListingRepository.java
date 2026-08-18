package com.example.petManagementService.pet.repository;

import com.example.petManagementService.pet.entity.AdoptionListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdoptionListingRepository extends JpaRepository<AdoptionListing, UUID> {
}
