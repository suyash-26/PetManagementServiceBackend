package com.example.petManagementService.CareCenter.entities;

import com.example.petManagementService.CareCenter.enums.MemberRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "center_members",uniqueConstraints = @UniqueConstraint(
        name = "uk_center_member",
        columnNames = {"center_id", "user_id"}
))
@Getter
@Setter
@NoArgsConstructor

public class CenterMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false)
    private CareCenter center;

    @Column(name = "user_id", nullable = false, updatable = false) // not a relationship of manyToOne as the microservice of auth is seperate
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole memberRole;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    @PrePersist
    public void prePersist() {
        joinedAt = Instant.now();
    }

}
