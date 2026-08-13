package com.example.petManagementService.CareCenter.exceptions;

public class DuplicateCenterException extends RuntimeException {
    public DuplicateCenterException(String message) {
        super(message);
    }
}
