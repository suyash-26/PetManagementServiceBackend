package com.example.petManagementService.pet.entity;

import com.example.petManagementService.pet.enums.ListingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "adoption_listings")
@Getter
@Setter
public class AdoptionListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Column(name = "center_id", nullable = false)
    private Long centerId;

    @Column(name = "listed_by_admin_id", nullable = false)
    private Long listedByAdminId;

    @Column(name = "source_intake_request_id")
    private Long sourceIntakeRequestId;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "adoption_fee")
    private BigDecimal adoptionFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_status", nullable = false)
    private ListingStatus listingStatus = ListingStatus.OPEN;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "posted_at", updatable = false)
    private Instant postedAt;
}