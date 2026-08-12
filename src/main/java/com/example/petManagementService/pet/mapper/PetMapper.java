package com.example.petManagementService.pet.mapper;

import com.example.petManagementService.pet.dto.PetRequest;
import com.example.petManagementService.pet.dto.PetResponse;
import com.example.petManagementService.pet.entity.Pet;
import org.springframework.stereotype.Component;

@Component
public class PetMapper {

    public Pet toEntity(PetRequest req) {
        Pet pet = new Pet();
        applyTo(req, pet);
        return pet;
    }

    public void applyTo(PetRequest req, Pet pet) {
        pet.setName(req.getName());
        pet.setSpecies(req.getSpecies());
        pet.setBreed(req.getBreed());
        pet.setGender(req.getGender());
        pet.setDateOfBirth(req.getDateOfBirth());
        pet.setSize(req.getSize());
        pet.setColor(req.getColor());
        pet.setWeightKg(req.getWeightKg());
        pet.setVaccinated(req.getVaccinated());
        pet.setSterilized(req.getSterilized());
        pet.setMedicalNotes(req.getMedicalNotes());
    }

    public PetResponse toResponse(Pet p) {
        return PetResponse.builder()
                .id(p.getId())
                .ownerUserId(p.getOwnerUserId())
                .custodianCenterId(p.getCustodianCenterId())
                .surrenderedByUserId(p.getSurrenderedByUserId())
                .name(p.getName())
                .species(p.getSpecies())
                .breed(p.getBreed())
                .gender(p.getGender())
                .dateOfBirth(p.getDateOfBirth())
                .size(p.getSize())
                .color(p.getColor())
                .weightKg(p.getWeightKg())
                .vaccinated(p.getVaccinated())
                .sterilized(p.getSterilized())
                .medicalNotes(p.getMedicalNotes())
                .status(p.getStatus())
                .version(p.getVersion())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}