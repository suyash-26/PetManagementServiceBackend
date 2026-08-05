package com.example.petManagementService.intake.controllers;

import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/intake")
public class IntakeRequestController {
    @PostMapping
    public void addIntakeRequest(){

    }
    @GetMapping("/centers/{id}/intake-requests")
    public ResponseEntity<String> getCentersIntakeRequest(@RequestParam String status){
        return ResponseEntity.ok("Fetched response");
    }
    @GetMapping("/centers/{centreId}/custody")
    public ResponseEntity<String> getAllPetsInCustodyForGivenCentre(@PathVariable UUID centreId){
        return ResponseEntity.ok("This is list of all pets that are in centres custody");
    }
}
