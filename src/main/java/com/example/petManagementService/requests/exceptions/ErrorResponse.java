package com.example.petManagementService.requests.exceptions;

import java.time.Instant;

// One consistent error shape for every failure mode in the requests/intake modules —
// callers can rely on {status, error, message, path} existing regardless of what
// actually went wrong, instead of some errors having a body and others not.
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
