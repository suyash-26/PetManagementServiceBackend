package com.example.petManagementService.BoardingRequests.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record BoardingRequestCreate(
        @NotNull UUID petId,
        @NotNull UUID centerId,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull @Future LocalDate endDate,
        @Size(max = 1000) String specialInstructions
) {}
