package com.example.petManagementService.BoardingRequests.repository;

import com.example.petManagementService.BoardingRequests.entities.BoardingRequests;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoardingRequestRepository extends JpaRepository<BoardingRequests, UUID> {
    List<BoardingRequests> findByRequest_CenterId(UUID centerId);  // Why Request_ with the underscore: centerId, requesterUserId, and petId are not columns on boarding_requests — they're on requests. The underscore means "go into the request association, then take centerId"
    List<BoardingRequests> findByRequest_RequesterUserId(UUID userId);
    List<BoardingRequests> findByRequest_PetId(UUID petId);
}
