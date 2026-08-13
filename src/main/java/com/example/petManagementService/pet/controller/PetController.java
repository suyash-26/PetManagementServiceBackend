package com.example.petManagementService.pet.controller;

import com.example.petManagementService.pet.dto.PetRequest;
import com.example.petManagementService.pet.dto.PetResponse;
import com.example.petManagementService.pet.service.PetService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.petManagementService.common.security.AuthenticatedUser;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @PostMapping
    public ResponseEntity<PetResponse> create(
            @Valid @RequestBody PetRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        PetResponse response = petService.create(request, user.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<PetResponse>> getMine(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(petService.getMine(user.id()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PetResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(petService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PetResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody PetRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(petService.update(id, request, user.id()));
    }
}