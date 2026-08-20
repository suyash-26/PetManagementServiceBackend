package com.example.petManagementService.BoardingRequests.repository;

import com.example.petManagementService.BoardingRequests.entities.BoardingRequests;
import com.example.petManagementService.requests.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BoardingRequestRepository extends JpaRepository<BoardingRequests, UUID> {

    // Request_ with the underscore: centerId, requesterUserId and petId are not columns on
    // boarding_requests — they're on requests. The underscore means "go into the request
    // association, then take centerId".
    List<BoardingRequests> findByRequest_CareCenter_Id(UUID centerId);

    // Long, not UUID: Request.requesterUserId is a Long (an authService user id). The old
    // UUID signature would have failed at bind time the first time it was called.
    List<BoardingRequests> findByRequest_RequesterUserId(Long userId);

    List<BoardingRequests> findByRequest_PetId(UUID petId);

    // Occupancy for a date window. Two intervals overlap when each starts on or before the
    // other ends — the standard test, and much easier to read as JPQL than as a derived
    // method name with four And-clauses.
    @Query("""
            select b from BoardingRequests b
            where b.request.careCenter.id = :centerId
              and b.request.status = :status
              and b.startDate <= :to
              and b.endDate >= :from
            """)
    List<BoardingRequests> findOverlapping(@Param("centerId") UUID centerId,
                                           @Param("status") RequestStatus status,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);
}