package com.example.petManagementService.controllers;

import com.example.petManagementService.dto.PetRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pets")
public class PetController {
    @PostMapping("")
    public ResponseEntity<String> addPet(@RequestBody PetRequest petRequest){
        return ResponseEntity.ok("pet added successfully");
    }
}
