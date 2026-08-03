package com.example.petManagementService.dto;

import com.example.petManagementService.enums.PetStatus;
import com.example.petManagementService.enums.Species;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class PetRequest {
    private String title;
    private String description;
    private Species species;
    private Integer age;
    private List<String> photos;
    private String notes;
    private PetStatus status;
    private Map<String,Object> attributes;
}
