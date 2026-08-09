package com.example.petManagementService.pet.repository;

import com.example.petManagementService.pet.entity.Pet;
import com.example.petManagementService.pet.enums.PetStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findByOwnerUserId(Long ownerUserId);

    List<Pet> findByOwnerUserIdAndStatus(Long ownerUserId, PetStatus status);

    List<Pet> findByCustodianCenterIdAndStatus(Long custodianCenterId, PetStatus status);

    List<Pet> findByCustodianCenterId(Long custodianCenterId);
}