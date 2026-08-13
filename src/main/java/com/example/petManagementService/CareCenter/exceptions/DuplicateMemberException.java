package com.example.petManagementService.CareCenter.exceptions;

public class DuplicateMemberException extends RuntimeException {
    public DuplicateMemberException(String message) {
        super(message);
    }
}
