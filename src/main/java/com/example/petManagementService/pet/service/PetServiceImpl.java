package com.example.petManagementService.pet.service;

import com.example.petManagementService.pet.dto.PetRequest;
import com.example.petManagementService.pet.dto.PetResponse;
import com.example.petManagementService.pet.entity.Pet;
import com.example.petManagementService.pet.enums.PetStatus;
import com.example.petManagementService.pet.mapper.PetMapper;
import com.example.petManagementService.pet.repository.PetRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final PetMapper petMapper;

    @Override
    @Transactional
    public PetResponse create(PetRequest request, Long currentUserId) {
        Pet pet = petMapper.toEntity(request);

        // Flow A: owner from JWT, no custodian, status OWNED.
        pet.setOwnerUserId(currentUserId);
        pet.setCustodianCenterId(null);
        pet.setStatus(PetStatus.OWNED);

        Pet saved = petRepository.save(pet);
        return petMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetResponse> getMine(Long currentUserId) {
        return petRepository.findByOwnerUserId(currentUserId)
                .stream()
                .map(petMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PetResponse getById(UUID petId) {
        Pet pet = findPetOrThrow(petId);
        return petMapper.toResponse(pet);
    }

    @Override
    @Transactional
    public PetResponse update(UUID petId, PetRequest request, Long currentUserId) {
        Pet pet = findPetOrThrow(petId);

        if (pet.getOwnerUserId() == null || !pet.getOwnerUserId().equals(currentUserId)) {
            throw new IllegalStateException("NOT_PET_OWNER");
        }

        petMapper.applyTo(request, pet);

        Pet saved = petRepository.save(pet);
        return petMapper.toResponse(saved);
    }

    private Pet findPetOrThrow(UUID petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("PET_NOT_FOUND"));
    }
}