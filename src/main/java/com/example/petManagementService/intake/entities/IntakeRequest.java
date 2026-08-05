package com.example.petManagementService.intake.entities;

import com.example.petManagementService.intake.enums.CustodyMode;
import com.example.petManagementService.intake.enums.IntakeReason;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class IntakeRequest {
    @Id
    @UuidGenerator
    private UUID request_id;
    private IntakeReason reason;
    private CustodyMode custodyMode;
    private Instant handover_time;
    private String ownerNotes;
    private String vetRecordsUrl;
}
