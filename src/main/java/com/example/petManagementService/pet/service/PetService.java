package com.example.petManagementService.pet.service;

import com.example.petManagementService.pet.dto.PetRequest;
import com.example.petManagementService.pet.dto.PetResponse;
import java.util.List;

public interface PetService {

    PetResponse create(PetRequest request, Long currentUserId);

    List<PetResponse> getMine(Long currentUserId);

    PetResponse getById(Long petId);

    PetResponse update(Long petId, PetRequest request, Long currentUserId);
}