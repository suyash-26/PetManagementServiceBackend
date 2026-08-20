package com.example.petManagementService.requests.entities;

import com.example.petManagementService.CareCenter.entities.CareCenter;
import com.example.petManagementService.requests.enums.RequestStatus;
import com.example.petManagementService.requests.enums.RequestType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

// Polymorphic parent shared by INTAKE/ADOPTION/BOARDING/GENERAL — see the matching
// detail table (IntakeRequest, AdoptionRequest, BoardingRequests) sharing this row's id
// via @MapsId. requesterUserId/assignedAdmin are Long, not UUID: they're references to
// authService user ids (Auth issues Long/bigint ids, not UUIDs) — Core never puts a FK
// on Auth's DB, it just trusts the id off the JWT.
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Request {
    @Id
    @UuidGenerator
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType requestType;

    @Column(nullable = false)
    private Long requesterUserId;

    private UUID petId;

    // NOT NULL per v2 §5 — "every request is owned by exactly one center". The engine
    // dereferences getCareCenter().getId() on every transition, so a null here is a 500
    // waiting to happen rather than a merely invalid row.
    @ManyToOne(optional = false)
    @JoinColumn(name = "care_center_id", nullable = false)
    private CareCenter careCenter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    private Long assignedAdmin;

    private String notes;

    // Optimistic lock — "blocks double-approval" per the design doc: two admins racing
    // to approve/reject/complete the same request will have one lose with a stale-object
    // exception instead of silently double-applying the transition.
    @Version
    private Integer version;

    @CreationTimestamp
    private Instant createdAt;

    private Instant decidedAt;
}
