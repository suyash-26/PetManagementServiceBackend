package com.example.petManagementService.requests.services;

import com.example.petManagementService.requests.repositories.RequestRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RequestService {
    private final RequestRepository requestRepository;

}
