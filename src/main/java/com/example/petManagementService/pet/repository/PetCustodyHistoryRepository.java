package com.example.petManagementService.pet.repository;

import com.example.petManagementService.pet.entity.PetCustodyHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PetCustodyHistoryRepository extends JpaRepository<PetCustodyHistory, UUID> {
}
