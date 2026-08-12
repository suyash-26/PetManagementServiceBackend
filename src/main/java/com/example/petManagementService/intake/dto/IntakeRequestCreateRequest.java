package com.example.petManagementService.intake.dto;

import com.example.petManagementService.intake.enums.CustodyMode;
import com.example.petManagementService.intake.enums.IntakeReason;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

// centerId (checked to exist, 404 if not — see IntakeRequestService) and petId (NOT
// checked — that needs the pet module, out of scope here) are both plain UUIDs on
// input; the response embeds the full CareCenter entity instead, once resolved.
public record IntakeRequestCreateRequest(
        @NotNull UUID petId,
        @NotNull UUID centerId,
        @NotNull IntakeReason reason,
        @NotNull CustodyMode custodyMode,
        LocalDate handoverDate,
        String ownerNotes,
        String vetRecordsUrl
) {
}
