package com.example.petManagementService.intake.repositories;

import com.example.petManagementService.intake.entities.IntakeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IntakeRequestRepository extends JpaRepository<IntakeRequest, UUID> {
}
