package com.example.petManagementService.CareCenter.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(DuplicateCenterException.class)
    public ResponseEntity<ApiError> handleDuplicateCenter(DuplicateCenterException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalTransitionException.class)
    public ResponseEntity<ApiError> handleIllegalTransition(IllegalTransitionException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(DuplicateMemberException.class)
    public ResponseEntity<ApiError> handleDuplicateCenterMember(DuplicateMemberException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(LastOwnerException.class)
    public ResponseEntity<ApiError> handleLastMember(LastOwnerException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(NotCenterAdminException.class)
    public ResponseEntity<ApiError> handleNotCenterAdmin(NotCenterAdminException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    // The intake, request and pet modules signal failures with ResponseStatusException
    // carrying a stable code as the reason (PET_HAS_ACTIVE_REQUEST, PET_NOT_FOUND,
    // NOT_PET_OWNER...). Spring leaves that reason out of the response body by default
    // (server.error.include-message=never), so a client could only see a bare status and
    // had to guess what went wrong. Re-emitting the reason in the ApiError envelope makes
    // those codes readable without exposing messages from unexpected 500s globally.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        return build(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR, ex.getReason(), null);
    }

    // Every @Valid failure on a request body lands here, so validation errors come back in
    // the same ApiError shape as everything else — with one entry per rejected field.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            // merge: a field with two broken constraints keeps the first message rather than throwing
            fieldErrors.merge(error.getField(), error.getDefaultMessage(), (first, second) -> first);
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", fieldErrors);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String msg, Map<String, String> fields) {
        return ResponseEntity.status(status)
                .body(new ApiError(LocalDateTime.now(), status.value(), msg, fields));
    }


}
