package com.example.petManagementService.pet.entity;

import com.example.petManagementService.pet.enums.TransferType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "pet_custody_history")
@Getter
@Setter
public class PetCustodyHistory {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(name = "from_user_id")
    private Long fromUserId;

    @Column(name = "from_center_id")
    private UUID fromCenterId;

    @Column(name = "to_user_id")
    private Long toUserId;

    @Column(name = "to_center_id")
    private UUID toCenterId;

    @Column(name = "request_id")
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false)
    private TransferType transferType;

    @Column(name = "transferred_at", nullable = false)
    private Instant transferredAt;

    @Column(name = "recorded_by")
    private Long recordedBy;
}