package com.example.petManagementService.CareCenter.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;


public record CareCenterRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 255) String description,
        @NotBlank @Size(max = 150) String city,
        @NotBlank @Size(max = 150) String state,
        @NotBlank @Size(max = 255) String address,
        @Email   @Size(max = 255) String contactEmail,
        @NotBlank @Size(max = 15) String  contactPhone,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @NotNull @Min(0) Integer capacity
        ) {
}
