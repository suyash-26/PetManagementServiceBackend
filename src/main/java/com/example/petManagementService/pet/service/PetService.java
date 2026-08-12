package com.example.petManagementService.pet.service;

import com.example.petManagementService.pet.dto.PetRequest;
import com.example.petManagementService.pet.dto.PetResponse;
import java.util.List;
import java.util.UUID;

public interface PetService {

    PetResponse create(PetRequest request, Long currentUserId);

    List<PetResponse> getMine(Long currentUserId);

    PetResponse getById(UUID petId);

    PetResponse update(UUID petId, PetRequest request, Long currentUserId);
}