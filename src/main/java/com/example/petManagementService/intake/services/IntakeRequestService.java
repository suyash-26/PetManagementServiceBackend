package com.example.petManagementService.intake.services;

import com.example.petManagementService.CareCenter.entities.CareCenter;
import com.example.petManagementService.CareCenter.enums.CenterStatus;
import com.example.petManagementService.CareCenter.repository.CareCenterRepository;
import com.example.petManagementService.intake.dto.IntakeRequestCreateRequest;
import com.example.petManagementService.intake.dto.IntakeRequestResponse;
import com.example.petManagementService.intake.entities.IntakeRequest;
import com.example.petManagementService.intake.mapper.IntakeRequestMapper;
import com.example.petManagementService.intake.repositories.IntakeRequestRepository;
import com.example.petManagementService.pet.mapper.PetMapper;
import com.example.petManagementService.pet.repository.PetRepository;
import com.example.petManagementService.pet.service.PetService;
import com.example.petManagementService.requests.entities.Request;
import com.example.petManagementService.requests.enums.RequestStatus;
import com.example.petManagementService.requests.enums.RequestType;
import com.example.petManagementService.requests.repositories.RequestRepository;
import com.example.petManagementService.requests.services.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.petManagementService.pet.dto.PetResponse;
import com.example.petManagementService.pet.enums.PetStatus;

import java.util.List;
import java.util.UUID;

// Flow B — hand a pet to a center. Creates the polymorphic Request (type=INTAKE) plus
// this module's own detail row, sharing one id via @MapsId.

// Cross-module reads (pet, center, membership) go through the owning module's *Service
// interface or repository as a read/existence check only — never a write into another
// module's data. The pet's actual mutation on completion belongs to the request engine,
// not here.
@Service
@RequiredArgsConstructor
public class IntakeRequestService {

    private final RequestRepository requestRepository;
    private final IntakeRequestRepository intakeRequestRepository;
    private final IntakeRequestMapper intakeRequestMapper;
    private final CareCenterRepository careCenterRepository;
    private final RequestService requestService;
    private final PetService petService;
    // The roster maps pet ENTITIES in bulk, which PetService's DTO-returning interface
    // can't provide — so the repository and mapper are used directly here.
    private final PetRepository petRepository;
    private final PetMapper petMapper;

    @Transactional
    public IntakeRequestResponse createIntake(Long requesterUserId, IntakeRequestCreateRequest dto) {
        // Flow B guard: "pet must be OWNED by the requester". Read through PetService
        // (not PetRepository) so this stays a cross-module read via the owning module's
        // interface. getById() already 404s PET_NOT_FOUND for an unknown id.

        PetResponse pet = petService.getById(dto.petId());

        // Null check first, and not merely defensive: a pet already in a center's custody
        // has ownerUserId == null, so equals() on it would NPE into a 500 instead of a 403.

        if (pet.getOwnerUserId() == null || !pet.getOwnerUserId().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NOT_PET_OWNER");
        }

        // Ownership alone isn't enough. A pet can still be owned by the requester while
        // mid-flight elsewhere — IN_BOARDING (owner keeps ownership, another center holds
        // the animal) or PENDING_INTAKE. Neither may be handed to a second center.

        if (pet.getStatus() != PetStatus.OWNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "PET_NOT_AVAILABLE_FOR_INTAKE: pet is " + pet.getStatus());
        }

        if (requestRepository.existsByPetIdAndStatusIn(dto.petId(), RequestService.ACTIVE_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PET_HAS_ACTIVE_REQUEST");
        }

        CareCenter center = careCenterRepository.findById(dto.centerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CENTER_NOT_FOUND"));
        if (center.getStatus() != CenterStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CENTER_NOT_ACTIVE");
        }

        Request request = intakeRequestMapper.toRequestEntity(requesterUserId, dto);
        request.setCareCenter(center);
        // saveAndFlush, not save: @CreationTimestamp only populates createdAt at flush
        // time, which a @Transactional method would otherwise defer until commit —
        // leaving createdAt null in *this* response even though it's correctly set by
        // the time anything re-reads the row afterward. Flushing now keeps the create
        // response accurate too.
        Request savedRequest = requestRepository.saveAndFlush(request);
        requestService.recordInitialCreation(savedRequest, requesterUserId);

        IntakeRequest intake = intakeRequestMapper.toIntakeEntity(dto);
        intake.setRequest(savedRequest); // @MapsId copies the id from savedRequest
        IntakeRequest savedIntake = intakeRequestRepository.save(intake);

        // GAP: "notify admins of that centre" (Flow B) — no notification module exists
        // yet, so this is a silent no-op today.

        return intakeRequestMapper.toResponse(savedRequest, savedIntake);
    }

    // Now scoped to admins of *this* center (or SUPER_ADMIN) via
    // RequestService.requireCenterAccess() — previously role-level only.
    @Transactional(readOnly = true)
    public List<IntakeRequestResponse> getCenterIntakeRequests(UUID centerId, RequestStatus status,
                                                                Long callerId, String callerRole) {
        requestService.requireCenterAccess(centerId, callerId, callerRole);

        List<Request> requests = status != null
                ? requestRepository.findByCareCenter_IdAndStatusAndRequestType(centerId, status, RequestType.INTAKE)
                : requestRepository.findByCareCenter_IdAndRequestType(centerId, RequestType.INTAKE);

        return requests.stream().map(this::toResponse).toList();
    }

    // v2 §8's custody roster: every pet this center is currently responsible for, whatever
    // state it's in. This is the entry point to Flow C — a listing may only be created for
    // a pet that is IN_CENTER_CUSTODY here, so this is the only view that shows which pets
    // are actually listable. Boarding guests appear too (the center holds them) but they
    // are still owned by their owners and can never be listed.
    @Transactional(readOnly = true)
    public List<PetResponse> getCustodyRoster(UUID centerId, Long callerId, String callerRole) {
        requestService.requireCenterAccess(centerId, callerId, callerRole);
        return petRepository.findByCustodianCenterId(centerId).stream()
                .map(petMapper::toResponse)
                .toList();
    }

    private IntakeRequestResponse toResponse(Request request) {
        IntakeRequest detail = intakeRequestRepository.findById(request.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "INTAKE_DETAIL_MISSING for " + request.getId()));
        return intakeRequestMapper.toResponse(request, detail);
    }
}
