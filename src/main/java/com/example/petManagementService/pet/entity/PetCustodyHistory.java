package com.example.petManagementService.pet.entity;

import com.example.petManagementService.pet.enums.TransferType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pet_custody_history")
@Getter
@Setter
public class PetCustodyHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Column(name = "from_user_id")
    private Long fromUserId;

    @Column(name = "from_center_id")
    private Long fromCenterId;

    @Column(name = "to_user_id")
    private Long toUserId;

    @Column(name = "to_center_id")
    private Long toCenterId;

    @Column(name = "request_id")
    private Long requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false)
    private TransferType transferType;

    @Column(name = "transferred_at", nullable = false)
    private Instant transferredAt;

    @Column(name = "recorded_by")
    private Long recordedBy;
}