package com.example.petManagementService.entities;

import com.example.petManagementService.enums.RequestStatus;
import com.example.petManagementService.enums.RequestType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

// Generalizes the interest / adoption / care flows into one table (mirrors the
// `attributes` JSON pattern on Pet): requestType picks the flavor, requestData carries
// whatever fields are specific to it (e.g. care -> startDate/endDate, interest -> phone).
@Entity
@Table(name = "requests")
@Getter
@Setter
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain reference into authService's User table — see Pet.ownerId for why this
    // isn't a @ManyToOne.
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private RequestType requestType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_data", columnDefinition = "json")
    private Map<String, Object> requestData;

    // request_pet junction table — a care request can plausibly cover more than one pet.
    @ManyToMany
    @JoinTable(
            name = "request_pet",
            joinColumns = @JoinColumn(name = "request_id"),
            inverseJoinColumns = @JoinColumn(name = "pet_id"))
    private Set<Pet> pets = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
