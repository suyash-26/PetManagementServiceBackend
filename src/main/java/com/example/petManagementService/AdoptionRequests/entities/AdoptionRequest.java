package com.example.petManagementService.AdoptionRequests.entities;

import com.example.petManagementService.requests.entities.Request;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "adoption_request")
@Getter
@Setter
@NoArgsConstructor
public class AdoptionRequest {
    @Id
    private UUID requestId;

    // Shares its PK with the owning Request row, same pattern as IntakeRequest and
    // BoardingRequests — don't generate this entity's id, copy it from the association.
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "request_id")
    private Request request;

    private UUID listingId;
    private UUID adopterUserId;
    private String message;
    private Boolean homeVisitStatus;
}
