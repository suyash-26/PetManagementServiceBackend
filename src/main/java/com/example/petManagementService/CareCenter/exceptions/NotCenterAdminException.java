package com.example.petManagementService.CareCenter.exceptions;

public class NotCenterAdminException extends RuntimeException {
    public NotCenterAdminException(String message) {
        super(message);
    }
}
