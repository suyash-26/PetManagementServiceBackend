package com.example.petManagementService.requests.repositories;

import com.example.petManagementService.requests.entities.Request;
import com.example.petManagementService.requests.enums.RequestStatus;
import com.example.petManagementService.requests.enums.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface RequestRepository extends JpaRepository<Request, UUID> {

    // "my requests" — requests(requester_user_id, status) per the doc's indexing cheatsheet
    List<Request> findByRequesterUserId(Long requesterUserId);

    // admin queue — requests(center_id, status, type) per the doc's indexing cheatsheet.
    // Navigates the careCenter relationship (Request no longer has a raw centerId column).
    List<Request> findByCareCenter_Id(UUID centerId);

    List<Request> findByCareCenter_IdAndStatus(UUID centerId, RequestStatus status);

    List<Request> findByCareCenter_IdAndRequestType(UUID centerId, RequestType requestType);

    List<Request> findByCareCenter_IdAndStatusAndRequestType(UUID centerId, RequestStatus status, RequestType requestType);

    // Intake/boarding guard: "pet must have no other active request" — active meaning
    // still in flight (not yet decided to a terminal state).
    boolean existsByPetIdAndStatusIn(UUID petId, Collection<RequestStatus> statuses);
}
