package com.example.petManagementService.requests.entities;

import com.example.petManagementService.requests.enums.RequestStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

// Immutable audit trail of every status transition — the doc's "request_status_history"
// entity, previously just a documented gap (getHistory() returned a fake single-entry
// snapshot). One row per transition, append-only, never updated or deleted.
@Entity
@Getter
@Setter
@NoArgsConstructor
public class RequestStatusHistory {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    // Null only for the very first row (request creation — there's no "from" state).
    @Enumerated(EnumType.STRING)
    private RequestStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private RequestStatus toStatus;

    // Long, not UUID — same as Request.requesterUserId/assignedAdmin: a reference to an
    // authService user id. Null would mean a system-initiated change, though nothing
    // does that today — every transition here is always someone's explicit action.
    private Long changedBy;

    @CreationTimestamp
    private Instant changedAt;

    private String notes;
}
