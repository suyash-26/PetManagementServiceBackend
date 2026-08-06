package com.example.petManagementService.CareCenter.entities;

import com.example.petManagementService.CareCenter.enums.CenterStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "care_centers")
@Getter
@Setter
@NoArgsConstructor
public class CareCenter {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false,length = 150)
    private String name;

    @Column(nullable = false,columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false,length = 150)
    private String city;

    @Column(nullable = false,length = 150)
    private String state;

    private String contactEmail;

    @Column(nullable = false,length = 15)
    private String contactPhone;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;
    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CenterStatus status = CenterStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist(){
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate(){
        updatedAt = Instant.now();
    }
}
