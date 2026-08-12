package com.example.petManagementService.CareCenter.dto;

import com.example.petManagementService.CareCenter.enums.MemberRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CenterMemberRequest(     // here we are not taking the centerId bcoz it will come from the path
        @NotNull Long userId,
        @NotNull MemberRole memberRole
) { }
