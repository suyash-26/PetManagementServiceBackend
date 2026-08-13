package com.example.petManagementService.CareCenter.repository;

import com.example.petManagementService.CareCenter.entities.CenterMember;
import com.example.petManagementService.CareCenter.enums.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CenterMemberRepository extends JpaRepository<CenterMember,UUID> {
   public boolean existsByCenter_IdAndUserId(UUID centerId, Long userId);
   public boolean existsByCenter_IdAndUserIdAndMemberRole(UUID centerId, Long userId, MemberRole role);
   public Optional<CenterMember> findByCenter_IdAndUserId(UUID centerId, Long userId);
   public List<CenterMember> findByCenter_Id(UUID centerId);
   public List<CenterMember> findByUserId(Long userId);
   public long countByCenter_IdAndMemberRole(UUID centerId, MemberRole role);
}
