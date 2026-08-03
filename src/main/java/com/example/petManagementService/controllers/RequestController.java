package com.example.petManagementService.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/requests")
public class RequestController {
    @PostMapping
    public ResponseEntity<String> addPet(@ReqestBody String data){

    }
}
