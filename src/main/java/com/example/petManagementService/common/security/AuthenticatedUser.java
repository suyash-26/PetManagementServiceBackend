package com.example.petManagementService.common.security;

// Built directly from JWT claims, not loaded from a database — Core has no `users`
// table (Auth owns identity; the cross-service rule forbids a FK back to Auth's DB or a
// call to Auth on the hot path). A verified token's claims are the entire principal,
// including `role` — the same SUPER_ADMIN / CENTER_ADMIN / USER value authService issued.
public record AuthenticatedUser(Long id, String email, String name, String role) {
}
