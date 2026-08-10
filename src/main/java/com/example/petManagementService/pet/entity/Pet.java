package com.example.petManagementService.pet.entity;

import com.example.petManagementService.pet.enums.Gender;
import com.example.petManagementService.pet.enums.PetStatus;
import com.example.petManagementService.pet.enums.Species;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "pets")
@Getter
@Setter
public class Pet {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "custodian_center_id")
    private UUID custodianCenterId;

    @Column(name = "surrendered_by_user_id")
    private Long surrenderedByUserId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Species species;

    private String breed;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String size;

    private String color;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    private Boolean vaccinated;

    private Boolean sterilized;

    @Column(name = "medical_notes", columnDefinition = "text")
    private String medicalNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PetStatus status = PetStatus.OWNED;

    @Version
    private Long version;

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PetImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "pet")
    private List<PetCustodyHistory> custodyHistory = new ArrayList<>();

    @OneToMany(mappedBy = "pet")
    private List<AdoptionListing> listings = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}