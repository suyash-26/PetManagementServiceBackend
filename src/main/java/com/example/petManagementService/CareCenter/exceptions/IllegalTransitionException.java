package com.example.petManagementService.CareCenter.exceptions;

public class IllegalTransitionException extends RuntimeException {
    public IllegalTransitionException(String message) {
        super(message);
    }
}
