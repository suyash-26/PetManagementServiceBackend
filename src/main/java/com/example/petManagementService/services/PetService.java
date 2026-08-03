package com.example.petManagementService.services;

import com.example.petManagementService.dto.PetRequest;
import com.example.petManagementService.dto.PetResponse;
import com.example.petManagementService.repositories.PetRepository;
import org.springframework.stereotype.Service;

@Service
public class PetService {
    private final PetRepository petRepository;
    public PetResponse addPet(PetRequest petRequest){

    }
}
