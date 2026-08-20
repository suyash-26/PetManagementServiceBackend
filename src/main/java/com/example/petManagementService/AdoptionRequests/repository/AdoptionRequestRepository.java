package com.example.petManagementService.AdoptionRequests.repository;

import com.example.petManagementService.AdoptionRequests.entities.AdoptionRequest;
import com.example.petManagementService.requests.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AdoptionRequestRepository extends JpaRepository<AdoptionRequest, UUID> {

    // Admin's applicant queue for one listing.
    List<AdoptionRequest> findByListingId(UUID listingId);

    // "Same user applies twice to one listing" guard (doc's DUPLICATE_APPLICATION).
    boolean existsByListingIdAndAdopterUserId(UUID listingId, Long adopterUserId);

    // Rival applicants to auto-reject when one is completed. Request_StatusIn with the
    // underscore: status lives on requests, not adoption_request, so Spring Data hops
    // through the request association.
    List<AdoptionRequest> findByListingIdAndRequest_StatusIn(
            UUID listingId, Collection<RequestStatus> statuses);
}
