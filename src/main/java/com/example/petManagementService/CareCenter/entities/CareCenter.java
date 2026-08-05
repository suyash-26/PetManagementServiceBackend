package com.example.petManagementService.CareCenter.entities;

import com.example.petManagementService.CareCenter.enums.CenterStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "care_center")
@Getter
@Setter
@NoArgsConstructor
public class CareCenter {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false,length = 150)
    private String name;

    @Column(nullable = false)
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

    private BigDecimal longitude;
    private BigDecimal latitude;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CenterStatus status = CenterStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;



}
