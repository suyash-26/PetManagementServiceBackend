package com.example.petManagementService.CareCenter.dto;

import com.example.petManagementService.CareCenter.enums.MemberRole;

import java.time.Instant;
import java.util.UUID;

public record CenterMemberResponse(
        UUID id,
        UUID centerId,
        Long userId,
        MemberRole memberRole,
        Instant joinedAt
) {}
