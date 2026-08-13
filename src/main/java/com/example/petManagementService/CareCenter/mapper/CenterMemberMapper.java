package com.example.petManagementService.CareCenter.mapper;

import com.example.petManagementService.CareCenter.dto.CenterMemberRequest;
import com.example.petManagementService.CareCenter.dto.CenterMemberResponse;
import com.example.petManagementService.CareCenter.entities.CareCenter;
import com.example.petManagementService.CareCenter.entities.CenterMember;
import org.springframework.stereotype.Component;

@Component
public class CenterMemberMapper {
    public CenterMember toEntity(CenterMemberRequest req, CareCenter center){
        CenterMember member = new CenterMember();
        member.setCenter(center);
        applyTo(req,member);
        return member;
    }

    public void applyTo(CenterMemberRequest req, CenterMember member){
        member.setUserId(req.userId());
        member.setMemberRole(req.memberRole());
    }

    public CenterMemberResponse toResponse(CenterMember member){
        return new CenterMemberResponse(
                member.getId(),member.getCenter().getId(),member.getUserId(),member.getMemberRole(),member.getJoinedAt()
        );
    }
}
