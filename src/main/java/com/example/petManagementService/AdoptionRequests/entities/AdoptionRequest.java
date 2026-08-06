package com.example.petManagementService.AdoptionRequests.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
    private UUID listingId;
    private UUID adopterUserId;
    private String message;
    private Boolean homeVisitStatus;
}
