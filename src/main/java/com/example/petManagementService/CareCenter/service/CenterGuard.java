package com.example.petManagementService.CareCenter.service;

import com.example.petManagementService.CareCenter.enums.MemberRole;
import com.example.petManagementService.CareCenter.exceptions.NotCenterAdminException;
import com.example.petManagementService.CareCenter.repository.CenterMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CenterGuard {

    @Autowired
   public CenterMemberRepository memberRepo;
    public void requireCenterAdmin(UUID centerId, Long userId) {
        if (!memberRepo.existsByCenter_IdAndUserId(centerId, userId)) {
            throw new NotCenterAdminException("NOT_CENTER_ADMIN");
        }
    }

    public void requireCenterOwner(UUID centerId, Long userId) {
        if (!memberRepo.existsByCenter_IdAndUserIdAndMemberRole(centerId, userId, MemberRole.OWNER)) {
            throw new NotCenterAdminException("NOT_CENTER_OWNER");
        }
    }
}
