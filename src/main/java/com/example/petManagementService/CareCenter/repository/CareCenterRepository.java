package com.example.petManagementService.CareCenter.repository;

import com.example.petManagementService.CareCenter.entities.CareCenter;
import com.example.petManagementService.CareCenter.enums.CenterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CareCenterRepository extends JpaRepository<CareCenter, UUID> {
   public List<CareCenter> findByStatus(CenterStatus status);
   public List<CareCenter> findByCityIgnoreCase(String city);
   public List<CareCenter> findByStatusAndCityIgnoreCase(CenterStatus status, String city);
   public boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);
}
