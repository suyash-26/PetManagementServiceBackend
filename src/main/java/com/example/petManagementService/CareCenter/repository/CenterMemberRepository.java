package com.example.petManagementService.CareCenter.repository;

import com.example.petManagementService.CareCenter.entities.CenterMember;
import com.example.petManagementService.CareCenter.enums.MemberRole;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CenterMemberRepository {
   public boolean existsByCenter_IdAndUserId(UUID centerId, UUID userId);
   public Optional<CenterMember> findByCenter_IdAndUserId(UUID centerId, UUID userId);
   public List<CenterMember> findByCenter_Id(UUID centerId);
   public List<CenterMember> findByUserId(UUID userId);
   public long countByCenter_IdAndMemberRole(UUID centerId, MemberRole role);
}
