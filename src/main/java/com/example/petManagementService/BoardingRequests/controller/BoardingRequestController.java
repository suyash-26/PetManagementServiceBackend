package com.example.petManagementService.BoardingRequests.controller;

import com.example.petManagementService.BoardingRequests.dto.BoardingRequestCreate;
import com.example.petManagementService.BoardingRequests.dto.BoardingRequestResponse;
import com.example.petManagementService.BoardingRequests.dto.CenterAvailabilityResponse;
import com.example.petManagementService.BoardingRequests.service.BoardingRequestService;
import com.example.petManagementService.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

// No class-level @RequestMapping: boarding requests live at /boarding-requests while
// availability hangs off the center, per v2 §8.
@RestController
@RequiredArgsConstructor
public class BoardingRequestController {

    private final BoardingRequestService boardingService;

    @PostMapping("/boarding-requests")
    public ResponseEntity<BoardingRequestResponse> create(
            @Valid @RequestBody BoardingRequestCreate request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardingService.create(currentUser.id(), request));
    }

    @PostMapping("/boarding-requests/{id}/check-in")
    public ResponseEntity<BoardingRequestResponse> checkIn(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(boardingService.checkIn(id, currentUser.id(), currentUser.role()));
    }

    @PostMapping("/boarding-requests/{id}/check-out")
    public ResponseEntity<BoardingRequestResponse> checkOut(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(boardingService.checkOut(id, currentUser.id(), currentUser.role()));
    }

    // @DateTimeFormat is required: without it Spring cannot bind ?from=2026-09-01 to a
    // LocalDate and answers 400 on every call.
    @GetMapping("/centers/{centerId}/availability")
    public ResponseEntity<CenterAvailabilityResponse> availability(
            @PathVariable UUID centerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(
                boardingService.availability(centerId, from, to, currentUser.id(), currentUser.role()));
    }
}