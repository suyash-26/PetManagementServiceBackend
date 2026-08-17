package com.example.petManagementService.requests.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.stream.Collectors;

// Scoped to just the requests/intake controllers via basePackages — deliberately does
// not touch how any other module's exceptions are handled.
//
// @Order(HIGHEST_PRECEDENCE): CareCenter.exceptions.GlobalExceptionHandler is a
// second, fully global (unscoped) @RestControllerAdvice that also handles
// MethodArgumentNotValidException. Neither advice declared an @Order, so which one
// actually ran was non-deterministic (Spring breaks ties by bean registration order).
// This only affects priority *when both are applicable to the same controller* — which
// only happens inside requests/intake, since that's this advice's basePackages scope.
// Every other module is unaffected; the global handler remains the only one that
// applies there.
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = {
        "com.example.petManagementService.requests",
        "com.example.petManagementService.intake"
})
public class RequestModuleExceptionHandler {

    // Every guard in RequestService/IntakeRequestService throws this — REQUEST_NOT_FOUND,
    // PET_HAS_ACTIVE_REQUEST, ILLEGAL_TRANSITION, NOT_REQUESTER_OR_ADMIN, CENTER_NOT_FOUND,
    // etc. Without this handler, ex.getReason() never reached the client at all
    // (server.error.include-message defaults to "never") — every one of those carefully
    // worded codes was invisible until now.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatusCode status = ex.getStatusCode();
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String error = resolved != null ? resolved.getReasonPhrase() : String.valueOf(status.value());
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(), status.value(), error, ex.getReason(), request.getRequestURI()
        ));
    }

    // @Valid failures on @RequestBody DTOs (e.g. IntakeRequestCreateRequest missing
    // petId/centerId/reason/custodyMode) — collects every field error instead of just
    // the first, so the caller can fix everything in one round trip.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ErrorResponse(
                Instant.now(), 400, "Bad Request", message, request.getRequestURI()
        ));
    }

    // Covers two distinct failure shapes that both mean "the request was malformed": an
    // enum value that doesn't exist (e.g. reason: "NOT_A_REAL_REASON" in the JSON body,
    // or ?status=NOTAREALSTATUS as a query param), or a body that isn't valid JSON at all.
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                Instant.now(), 400, "Bad Request", "Malformed request: " + ex.getMessage(), request.getRequestURI()
        ));
    }

    // The @Version field on Request exists specifically to block double-approval races
    // (two admins racing to approve/reject/complete the same request) — but nothing was
    // catching the exception it actually throws, so a real race would 500 instead of
    // failing with the clean "someone else already changed this, refetch and retry"
    // response the field was added for.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                Instant.now(), 409, "Conflict",
                "CONCURRENT_UPDATE: this request was changed by someone else at the same time — refetch and retry.",
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                Instant.now(), 400, "Bad Request", ex.getMessage(), request.getRequestURI()
        ));
    }

    // @PreAuthorize denials (e.g. hasAnyRole('CENTER_ADMIN','SUPER_ADMIN') failing, or
    // RequestService.requireCenterAccess() throwing) surface as this — or, in Spring
    // Security 6/7's newer method-security, its subclass AuthorizationDeniedException.
    // Without this explicit handler it would fall through to the generic Exception
    // catch-all below and wrongly become a 500 instead of a 403.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
                Instant.now(), 403, "Forbidden", "You don't have permission to do this.", request.getRequestURI()
        ));
    }

    // Last resort: never let an unexpected exception leak a stack trace or an
    // unstructured body to the client. Logged server-side so it's still debuggable.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception in requests/intake module", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                Instant.now(), 500, "Internal Server Error",
                "Something went wrong. Please try again.",
                request.getRequestURI()
        ));
    }
}
