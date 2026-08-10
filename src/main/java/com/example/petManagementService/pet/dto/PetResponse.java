package com.example.petManagementService.pet.dto;

import com.example.petManagementService.pet.enums.Gender;
import com.example.petManagementService.pet.enums.PetStatus;
import com.example.petManagementService.pet.enums.Species;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PetResponse {

    private UUID id;

    private Long ownerUserId;
    private UUID custodianCenterId;
    private Long surrenderedByUserId;

    private String name;
    private Species species;
    private String breed;
    private Gender gender;
    private LocalDate dateOfBirth;

    private String size;
    private String color;
    private BigDecimal weightKg;

    private Boolean vaccinated;
    private Boolean sterilized;
    private String medicalNotes;

    private PetStatus status;
    private Long version;

    private Instant createdAt;
    private Instant updatedAt;
}