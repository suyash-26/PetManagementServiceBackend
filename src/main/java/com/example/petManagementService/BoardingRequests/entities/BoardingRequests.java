package com.example.petManagementService.BoardingRequests.entities;

import com.example.petManagementService.requests.entities.Request;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "boarding_requests")
@Getter
@Setter
@NoArgsConstructor
public class BoardingRequests {
    @Id
    public UUID requestId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId  // don't generate this entity's id — copy it from the associated Request
    @JoinColumn(name = "request_id")
    private Request request;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(columnDefinition = "text")
    private String specialInstructions;

    @Column(precision = 10, scale = 2)
    private BigDecimal quotedPrice;

    private Instant checkedInAt;   //  date (Date) vs timestamptz.(exact timestamps)
    private Instant checkedOutAt;
}
