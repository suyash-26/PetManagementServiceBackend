package com.example.petManagementService.AdoptionRequests.controllers;

import com.example.petManagementService.AdoptionRequests.entities.AdoptionRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/adoptions")
public class AdoptionRequestController {
    @GetMapping
    public ResponseEntity<List<AdoptionRequest>> getAllAdoptionRequests(){
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping("/create")
    public ResponseEntity<String> createAdoptionRequest(@RequestBody AdoptionRequest adoptionRequest){
        return ResponseEntity.ok("Adoption request created");
    }
}
