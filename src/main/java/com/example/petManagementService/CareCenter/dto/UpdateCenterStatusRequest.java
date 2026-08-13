package com.example.petManagementService.CareCenter.dto;

import com.example.petManagementService.CareCenter.enums.CenterStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCenterStatusRequest(@NotNull CenterStatus status) {}