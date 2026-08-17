package com.example.petManagementService.CareCenter.controller;

import com.example.petManagementService.CareCenter.dto.*;
import com.example.petManagementService.CareCenter.enums.CenterStatus;
import com.example.petManagementService.CareCenter.service.CareCenterService;
import com.example.petManagementService.CareCenter.service.CenterGuard;
import com.example.petManagementService.CareCenter.service.CenterMemberService;
import com.example.petManagementService.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/centers")
@RequiredArgsConstructor
public class CareCenterController {

    private final CareCenterService centerService;
    private final CenterMemberService memberService;
    private final CenterGuard centerGuard;

    @PostMapping
    public ResponseEntity<CareCenterResponse> create(
            @Valid @RequestBody CareCenterRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(centerService.createCareCenter(request, user.id()));
    }

    // public feed — ACTIVE only, hardcoded so no query param can widen it
    @GetMapping
    public ResponseEntity<List<CareCenterResponse>> search(@RequestParam(required = false) String city) {
        return ResponseEntity.ok(centerService.search(city, CenterStatus.ACTIVE));
    }

    // Admin listing. The public feed above can never return a PENDING center, which left
    // approval unreachable: an admin had to already know a center's UUID to act on it.
    // Same search, but the status is the caller's to choose (omit it for every center),
    // which is exactly why this one is gated to SUPER_ADMIN and the public route isn't.
    @GetMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<CareCenterResponse>> adminSearch(
            @RequestParam(required = false) CenterStatus status,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(centerService.search(city, status));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<CenterMemberResponse>> myCenters(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(memberService.myCenters(user.id()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CareCenterResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(centerService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CareCenterResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CareCenterRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        centerGuard.requireCenterAdmin(id, user.id());
        return ResponseEntity.ok(centerService.updateCareCenter(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CareCenterResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCenterStatusRequest body) {
        return ResponseEntity.ok(centerService.updateStatus(id, body.status()));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<CenterMemberResponse> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody CenterMemberRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        centerGuard.requireCenterOwner(id, user.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberService.createCenterMember(request, id));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<CenterMemberResponse>> listMembers(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        centerGuard.requireCenterAdmin(id, user.id());
        return ResponseEntity.ok(memberService.listMembers(id));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        centerGuard.requireCenterOwner(id, user.id());
        memberService.removeMember(id, memberId);
        return ResponseEntity.noContent().build();
    }
}