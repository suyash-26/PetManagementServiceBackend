package com.example.petManagementService.pet.dto;

import com.example.petManagementService.pet.enums.Gender;
import com.example.petManagementService.pet.enums.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PetRequest {

    @NotBlank
    private String name;

    @NotNull
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

}