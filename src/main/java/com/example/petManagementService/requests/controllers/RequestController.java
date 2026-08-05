package com.example.petManagementService.requests.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/requests")
public class RequestController {
    @GetMapping("/mine")
    public ResponseEntity<String> getMyAllRequests(){
        return ResponseEntity.ok("this is userid from jwt requests");
    }
    @GetMapping("/centers/{id}/requests")
    public ResponseEntity<String> getAllRequestAgainstThisCenter(){
        return ResponseEntity.ok("this is all requests tagged to given center id");
    }
    @PostMapping("/{id}/approve")
    public ResponseEntity<String> approveRequest(){
        return ResponseEntity.ok("Requests approved");
    }
    @PostMapping("/{id}/reject")
    public ResponseEntity<String> rejectRequest(){
        return ResponseEntity.ok("Requests approved");
    }
    @PostMapping("/{id}/cancel")
    public ResponseEntity<String> cancelRequest(){
        return ResponseEntity.ok("Requests approved");
    }
    @PostMapping("/{id}/complete")
    public ResponseEntity<String> completeRequest(){
        return ResponseEntity.ok("Requests approved");
    }
    @GetMapping("/{id}/history")
    public ResponseEntity<String> getRequestHistory(){
        return ResponseEntity.ok("this is complete history of this request");
    }
}
