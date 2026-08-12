package com.example.petManagementService.intake.entities;

import com.example.petManagementService.intake.enums.CustodyMode;
import com.example.petManagementService.intake.enums.IntakeReason;
import com.example.petManagementService.requests.entities.Request;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// Detail row for a hand-a-pet-to-a-center request. Shares its PK with the owning
// Request row via @MapsId — don't generate this entity's id, copy it from the
// association — same pattern as BoardingRequests/AdoptionRequest.
@Entity
@Getter
@Setter
@NoArgsConstructor
public class IntakeRequest {
    @Id
    private UUID requestId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "request_id")
    private Request request;

    @Enumerated(EnumType.STRING)
    private IntakeReason reason;

    @Enumerated(EnumType.STRING)
    private CustodyMode custodyMode;

    // Desired/requested handover date, set by the owner at creation — drives the
    // stale-PENDING_INTAKE sweep. Distinct from handoverCompletedAt below.
    private LocalDate handoverDate;

    private String ownerNotes;
    private String vetRecordsUrl;

    // Set only when the admin actually completes the request (physical handover done),
    // not when it's merely approved — approval is a promise, this is the real event.
    private Instant handoverCompletedAt;
}
