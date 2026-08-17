package com.example.petManagementService.requests.repositories;

import com.example.petManagementService.requests.entities.RequestStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequestStatusHistoryRepository extends JpaRepository<RequestStatusHistory, UUID> {

    List<RequestStatusHistory> findByRequest_IdOrderByChangedAtAsc(UUID requestId);
}
