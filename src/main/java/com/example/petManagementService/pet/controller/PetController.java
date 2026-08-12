package com.example.petManagementService.pet.controller;

import com.example.petManagementService.pet.dto.PetRequest;
import com.example.petManagementService.pet.dto.PetResponse;
import com.example.petManagementService.pet.service.PetService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @PostMapping
    public ResponseEntity<PetResponse> create(
            @Valid @RequestBody PetRequest request,
            Principal principal) {

        Long currentUserId = currentUserId(principal);
        PetResponse response = petService.create(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<PetResponse>> getMine(Principal principal) {
        Long currentUserId = currentUserId(principal);
        return ResponseEntity.ok(petService.getMine(currentUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PetResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(petService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PetResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody PetRequest request,
            Principal principal) {

        Long currentUserId = currentUserId(principal);
        return ResponseEntity.ok(petService.update(id, request, currentUserId));
    }

    private Long currentUserId(Principal principal) {
        return Long.valueOf(principal.getName());
    }
}