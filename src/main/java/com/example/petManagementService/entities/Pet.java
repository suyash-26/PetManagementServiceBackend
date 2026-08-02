package com.example.petManagementService.entities;

import com.example.petManagementService.enums.Gender;
import com.example.petManagementService.enums.PetStatus;
import com.example.petManagementService.enums.Species;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pets")
@Getter
@Setter
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Species species;

    // Species-specific fields (breed/vaccinated for dog & cat, tank size/water type
    // for fish, etc.) live here instead of one column per possible attribute.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> attributes;

    // Plain reference, not a @ManyToOne: User is owned by authService, a separate
    // microservice/database — no local join possible or appropriate here. Null when
    // the pet is center-sourced (e.g. an admin-uploaded sale listing) rather than
    // uploaded by one of the app's own users. Resolve to a display name via authService's
    // API when needed, not via a local relation.
    @Column(name = "owner_id")
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Integer age;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> photos;

    // Added on top of the original diagram: without this, nothing distinguished a plain
    // pet profile from one actively listed for adoption. No price/sale concept — this is
    // an NGO adoption flow, not a marketplace.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PetStatus status = PetStatus.NOT_LISTED;

    private String notes;

    private boolean trained;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
