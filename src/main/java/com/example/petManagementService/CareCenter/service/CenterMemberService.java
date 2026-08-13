package com.example.petManagementService.CareCenter.service;

import com.example.petManagementService.CareCenter.dto.CareCenterResponse;
import com.example.petManagementService.CareCenter.dto.CenterMemberRequest;
import com.example.petManagementService.CareCenter.dto.CenterMemberResponse;
import com.example.petManagementService.CareCenter.entities.CareCenter;
import com.example.petManagementService.CareCenter.entities.CenterMember;
import com.example.petManagementService.CareCenter.enums.MemberRole;
import com.example.petManagementService.CareCenter.exceptions.DuplicateMemberException;
import com.example.petManagementService.CareCenter.exceptions.LastOwnerException;
import com.example.petManagementService.CareCenter.exceptions.ResourceNotFoundException;
import com.example.petManagementService.CareCenter.mapper.CenterMemberMapper;
import com.example.petManagementService.CareCenter.repository.CareCenterRepository;
import com.example.petManagementService.CareCenter.repository.CenterMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CenterMemberService {
    @Autowired
    public CareCenterRepository centerRepo;
    @Autowired
    public CenterMemberRepository memberRepo;
    @Autowired
    public CenterMemberMapper mapper;

    @Transactional
    public CenterMemberResponse createCenterMember(CenterMemberRequest req, UUID centerId){

        CareCenter center = centerRepo.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("CENTER_NOT_FOUND"));

        if (memberRepo.existsByCenter_IdAndUserId(centerId, req.userId())) {
            throw new DuplicateMemberException("MEMBER_ALREADY_EXISTS");
        }

        CenterMember member = mapper.toEntity(req, center);
        return mapper.toResponse(memberRepo.save(member));
    }

    public List<CenterMemberResponse> listMembers(UUID centerId){
        return memberRepo.findByCenter_Id(centerId).stream().map(mapper::toResponse).toList();
    }

    public List<CenterMemberResponse> myCenters(Long userId) {
        return memberRepo.findByUserId(userId).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public void removeMember(UUID centerId, UUID memberId) {

        CenterMember member = memberRepo.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND"));

        // the membership must belong to the center in the path — otherwise an admin of
        // center A could delete a membership in center B just by guessing its id
        if (!member.getCenter().getId().equals(centerId)) {
            throw new ResourceNotFoundException("MEMBER_NOT_FOUND");
        }

        if (member.getMemberRole() == MemberRole.OWNER
                && memberRepo.countByCenter_IdAndMemberRole(centerId, MemberRole.OWNER) == 1) {
            throw new LastOwnerException("CANNOT_REMOVE_LAST_OWNER");
        }

        memberRepo.delete(member);
    }



}
