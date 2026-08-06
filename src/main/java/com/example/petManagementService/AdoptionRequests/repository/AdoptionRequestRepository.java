package com.example.petManagementService.AdoptionRequests.repository;

import com.example.petManagementService.AdoptionRequests.entities.AdoptionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdoptionRequestRepository extends JpaRepository<AdoptionRequest, UUID> {
}
