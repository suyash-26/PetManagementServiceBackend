package com.example.petManagementService.CareCenter.dto;

import com.example.petManagementService.CareCenter.enums.CenterStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CareCenterResponse(
        UUID id, String name, String description, String address, String city, String state, String contactEmail, String contactPhone
        , BigDecimal latitude, BigDecimal longitude, Integer capacity, CenterStatus status, UUID createdBy, Instant createdAt, Instant updatedAt
        ) {
}
