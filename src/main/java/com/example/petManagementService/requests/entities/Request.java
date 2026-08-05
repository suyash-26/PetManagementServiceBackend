package com.example.petManagementService.requests.entities;

import com.example.petManagementService.requests.enums.RequestStatus;
import com.example.petManagementService.requests.enums.RequestType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Request {
    @Id
    @UuidGenerator
    private UUID id;
    private RequestType requestType;
    private UUID requesterUserId;
    private UUID petId;
    private UUID centerId;
    private RequestStatus status;
    private UUID assignedAdmin;
    private String notes;
    private Integer version;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant decidedAt;
}
