package com.example.petManagementService.repositories;

import com.example.petManagementService.entities.Request;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestRepository extends JpaRepository<Request,Long> {
}
