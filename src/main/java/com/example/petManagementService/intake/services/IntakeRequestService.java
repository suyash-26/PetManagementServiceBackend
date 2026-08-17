package com.example.petManagementService.intake.services;

import com.example.petManagementService.CareCenter.entities.CareCenter;
import com.example.petManagementService.CareCenter.enums.CenterStatus;
import com.example.petManagementService.CareCenter.repository.CareCenterRepository;
import com.example.petManagementService.intake.dto.IntakeRequestCreateRequest;
import com.example.petManagementService.intake.dto.IntakeRequestResponse;
import com.example.petManagementService.intake.entities.IntakeRequest;
import com.example.petManagementService.intake.mapper.IntakeRequestMapper;
import com.example.petManagementService.intake.repositories.IntakeRequestRepository;
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

import java.util.List;
import java.util.UUID;

// Flow B — hand a pet to a center. Creates the polymorphic Request (type=INTAKE) plus
// this module's own detail row, sharing one id via @MapsId.
//
// Deliberately does NOT touch the pet module (out of scope for this pass): petId is
// taken as an opaque UUID reference, with no check that the pet exists or is OWNED by
// the caller. CareCenterRepository/CenterMemberRepository (via RequestService) ARE
// used, but only as lightweight, unmodified dependencies — reads/existence checks
// against already-correct repositories, never a write into another module's data.
@Service
@RequiredArgsConstructor
public class IntakeRequestService {

    private final RequestRepository requestRepository;
    private final IntakeRequestRepository intakeRequestRepository;
    private final IntakeRequestMapper intakeRequestMapper;
    private final CareCenterRepository careCenterRepository;
    private final RequestService requestService;

    @Transactional
    public IntakeRequestResponse createIntake(Long requesterUserId, IntakeRequestCreateRequest dto) {
        // GAP: doc also requires "pet must be OWNED by the requester" here — needs the
        // pet module. Only the request-level guard (no other active request on this
        // pet) is enforced.
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

    private IntakeRequestResponse toResponse(Request request) {
        IntakeRequest detail = intakeRequestRepository.findById(request.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "INTAKE_DETAIL_MISSING for " + request.getId()));
        return intakeRequestMapper.toResponse(request, detail);
    }
}
