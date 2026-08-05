package com.example.petManagementService.intake.entities;

import com.example.petManagementService.intake.enums.CustodyMode;
import com.example.petManagementService.intake.enums.IntakeReason;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class IntakeRequest {
    @Id
    private UUID request_id;
    private IntakeReason reason;
    private CustodyMode custodyMode;
    private Instant handover_time;
}
