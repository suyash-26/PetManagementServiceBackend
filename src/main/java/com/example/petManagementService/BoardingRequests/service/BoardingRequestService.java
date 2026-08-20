package com.example.petManagementService.BoardingRequests.service;

import com.example.petManagementService.BoardingRequests.dto.BoardingRequestCreate;
import com.example.petManagementService.BoardingRequests.dto.BoardingRequestResponse;
import com.example.petManagementService.BoardingRequests.dto.CenterAvailabilityResponse;
import com.example.petManagementService.BoardingRequests.entities.BoardingRequests;
import com.example.petManagementService.BoardingRequests.mapper.BoardingRequestMapper;
import com.example.petManagementService.BoardingRequests.repository.BoardingRequestRepository;
import com.example.petManagementService.CareCenter.entities.CareCenter;
import com.example.petManagementService.CareCenter.enums.CenterStatus;
import com.example.petManagementService.CareCenter.repository.CareCenterRepository;
import com.example.petManagementService.pet.dto.PetResponse;
import com.example.petManagementService.pet.entity.Pet;
import com.example.petManagementService.pet.enums.PetStatus;
import com.example.petManagementService.pet.repository.PetRepository;
import com.example.petManagementService.pet.service.CustodyService;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Flow E — temporary care. Approve/reject/cancel run through the generic RequestController;
// this module owns creation, availability, and the two physical events (check-in/out).
@Service
@RequiredArgsConstructor
public class BoardingRequestService {

    private final BoardingRequestRepository boardingRepository;
    private final BoardingRequestMapper boardingMapper;
    private final RequestRepository requestRepository;
    private final RequestService requestService;
    private final CareCenterRepository careCenterRepository;
    private final PetService petService;
    private final PetRepository petRepository;
    private final CustodyService custodyService;

    @Transactional
    public BoardingRequestResponse create(Long requesterUserId, BoardingRequestCreate dto) {
        // Same ownership guard as intake: the pet must be OWNED by the requester and not
        // mid-flight anywhere else.
        PetResponse pet = petService.getById(dto.petId());
        if (pet.getOwnerUserId() == null || !pet.getOwnerUserId().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NOT_PET_OWNER");
        }
        if (pet.getStatus() != PetStatus.OWNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "PET_NOT_AVAILABLE_FOR_BOARDING: pet is " + pet.getStatus());
        }

        // @Future/@FutureOrPresent on the DTO validate each date alone; neither can express
        // "end must follow start", so that check lives here.
        if (!dto.endDate().isAfter(dto.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_RANGE: endDate must be after startDate");
        }

        if (requestRepository.existsByPetIdAndStatusIn(dto.petId(), RequestService.ACTIVE_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PET_HAS_ACTIVE_REQUEST");
        }

        CareCenter center = careCenterRepository.findById(dto.centerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CENTER_NOT_FOUND"));
        if (center.getStatus() != CenterStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CENTER_NOT_ACTIVE");
        }

        Request request = new Request();
        request.setRequestType(RequestType.BOARDING);
        request.setRequesterUserId(requesterUserId);
        request.setPetId(dto.petId());
        request.setCareCenter(center);
        request.setStatus(RequestStatus.PENDING);
        Request savedRequest = requestRepository.saveAndFlush(request);
        requestService.recordInitialCreation(savedRequest, requesterUserId);

        BoardingRequests detail = boardingMapper.toEntity(dto);
        detail.setRequest(savedRequest);   // @MapsId copies the id
        return boardingMapper.toResponse(boardingRepository.save(detail));
    }

    // The center's boarding queue. Exists alongside the generic request queue because
    // RequestResponse carries no startDate, endDate, checkedInAt or checkedOutAt — so
    // reviewing boarding from there would mean approving a stay without seeing its dates.
    @Transactional(readOnly = true)
    public List<BoardingRequestResponse> getCenterBoardingRequests(UUID centerId, RequestStatus status,
                                                                    Long callerId, String callerRole) {
        requestService.requireCenterAccess(centerId, callerId, callerRole);

        List<BoardingRequests> rows = status != null
                ? boardingRepository.findByRequest_CareCenter_IdAndRequest_StatusOrderByStartDateAsc(centerId, status)
                : boardingRepository.findByRequest_CareCenter_IdOrderByStartDateAsc(centerId);

        return rows.stream().map(boardingMapper::toResponse).toList();
    }

    // Flow E step 3 — drop-off. Only from APPROVED: you cannot check in an animal whose
    // stay was never agreed.
    @Transactional
    public BoardingRequestResponse checkIn(UUID requestId, Long adminUserId, String callerRole) {
        BoardingRequests detail = findDetail(requestId);
        Request request = detail.getRequest();
        requestService.requireCenterAccess(request.getCareCenter().getId(), adminUserId, callerRole);

        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "ILLEGAL_TRANSITION: cannot check in a " + request.getStatus() + " boarding request");
        }
        if (detail.getCheckedInAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ALREADY_CHECKED_IN");
        }

        custodyService.boardingCheckIn(request.getPetId(), request.getCareCenter().getId(),
                requestId, adminUserId);
        detail.setCheckedInAt(Instant.now());
        return boardingMapper.toResponse(boardingRepository.save(detail));
    }

    // Flow E step 5 — pick-up. Clears custody, then finishes the request through the shared
    // engine so its status and history stay consistent with every other request type.
    @Transactional
    public BoardingRequestResponse checkOut(UUID requestId, Long adminUserId, String callerRole) {
        BoardingRequests detail = findDetail(requestId);
        Request request = detail.getRequest();
        requestService.requireCenterAccess(request.getCareCenter().getId(), adminUserId, callerRole);

        if (detail.getCheckedInAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "NOT_CHECKED_IN: cannot check out a pet that was never checked in");
        }
        if (detail.getCheckedOutAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ALREADY_CHECKED_OUT");
        }

        custodyService.boardingCheckOut(request.getPetId(), requestId, adminUserId);
        detail.setCheckedOutAt(Instant.now());
        BoardingRequests saved = boardingRepository.save(detail);

        // Reuse the state machine rather than setting COMPLETED by hand — that keeps the
        // status-history row and the optimistic-lock check identical to every other type.
        // RequestService.complete()'s switch has no BOARDING branch on purpose: the custody
        // effect already happened here.
        requestService.complete(requestId, adminUserId, callerRole);

        return boardingMapper.toResponse(saved);
    }

    // v2 §6.3. Two sources of occupancy: boardings overlapping the window, and pets the
    // center is physically holding.
    @Transactional(readOnly = true)
    public CenterAvailabilityResponse availability(UUID centerId, LocalDate from, LocalDate to,
                                                   Long callerUserId, String callerRole) {
        requestService.requireCenterAccess(centerId, callerUserId, callerRole);

        CareCenter center = careCenterRepository.findById(centerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CENTER_NOT_FOUND"));
        if (!to.isAfter(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_RANGE: 'to' must be after 'from'");
        }

        int boardings = boardingRepository
                .findOverlapping(centerId, RequestStatus.APPROVED, from, to).size();

        // Pets in custody count toward kennel occupancy. RESERVED and
        // AVAILABLE_FOR_ADOPTION are included because the animal is still physically there.
        // NOTE: this over-counts FOSTER_IN_PLACE intakes, which consume no kennel slot —
        // fixing that needs the physically_present flag the pet entity doesn't have yet.
        int inCustody = (int) petRepository.findByCustodianCenterId(centerId).stream()
                .map(Pet::getStatus)
                .filter(s -> s == PetStatus.IN_CENTER_CUSTODY
                        || s == PetStatus.AVAILABLE_FOR_ADOPTION
                        || s == PetStatus.RESERVED)
                .count();

        int capacity = center.getCapacity() != null ? center.getCapacity() : 0;
        return new CenterAvailabilityResponse(centerId, from, to, capacity,
                boardings, inCustody, Math.max(0, capacity - boardings - inCustody));
    }

    private BoardingRequests findDetail(UUID requestId) {
        return boardingRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "BOARDING_REQUEST_NOT_FOUND"));
    }
}