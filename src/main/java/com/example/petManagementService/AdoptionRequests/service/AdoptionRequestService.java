package com.example.petManagementService.AdoptionRequests.service;

import com.example.petManagementService.AdoptionRequests.dto.AdoptionRequestCreateRequest;
import com.example.petManagementService.AdoptionRequests.dto.AdoptionRequestResponse;
import com.example.petManagementService.AdoptionRequests.entities.AdoptionRequest;
import com.example.petManagementService.AdoptionRequests.mapper.AdoptionRequestMapper;
import com.example.petManagementService.AdoptionRequests.repository.AdoptionRequestRepository;
import com.example.petManagementService.CareCenter.repository.CareCenterRepository;
import com.example.petManagementService.pet.entity.AdoptionListing;
import com.example.petManagementService.pet.enums.ListingStatus;
import com.example.petManagementService.pet.repository.AdoptionListingRepository;
import com.example.petManagementService.requests.entities.Request;
import com.example.petManagementService.requests.repositories.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

// Flow D — an adopter applies to a center-created listing. Creates the polymorphic
// Request (type=ADOPTION) plus this module's own detail row, sharing one id via
// @MapsId — same pattern as IntakeRequestService.
//
// Deliberately does NOT touch approve/reject/cancel/complete or any pet/listing
// custody transfer (out of scope for this pass): those keep running through the
// existing generic RequestService/RequestController, unchanged.
@Service
@RequiredArgsConstructor
public class AdoptionRequestService {

    private final RequestRepository requestRepository;
    private final AdoptionRequestRepository adoptionRequestRepository;
    private final AdoptionRequestMapper adoptionRequestMapper;
    private final AdoptionListingRepository adoptionListingRepository;
    private final CareCenterRepository careCenterRepository;

    @Transactional
    public AdoptionRequestResponse applyToListing(Long adopterUserId, UUID listingId, AdoptionRequestCreateRequest dto) {
        AdoptionListing listing = adoptionListingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LISTING_NOT_FOUND"));

        if (listing.getListingStatus() != ListingStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "LISTING_NOT_OPEN");
        }

        // No "pet has no other active request" guard here, unlike intake — competing
        // applicants on one listing is the intended shape (Flow D), only a duplicate
        // application from the same adopter is rejected.
        if (adoptionRequestRepository.existsByListingIdAndAdopterUserId(listingId, adopterUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DUPLICATE_APPLICATION");
        }

        Request request = adoptionRequestMapper.toRequestEntity(adopterUserId, listing.getPet().getId(), dto);
        request.setCareCenter(careCenterRepository.findById(listing.getCenterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CENTER_NOT_FOUND")));
        // saveAndFlush, not save: @CreationTimestamp only populates createdAt at flush
        // time, which a @Transactional method would otherwise defer until commit.
        Request savedRequest = requestRepository.saveAndFlush(request);

        AdoptionRequest detail = adoptionRequestMapper.toDetailEntity(dto);
        detail.setRequest(savedRequest); // @MapsId copies the id from savedRequest
        detail.setListingId(listingId);
        detail.setAdopterUserId(adopterUserId);
        AdoptionRequest savedDetail = adoptionRequestRepository.save(detail);

        return adoptionRequestMapper.toResponse(savedRequest, savedDetail);
    }

    @Transactional(readOnly = true)
    public List<AdoptionRequestResponse> getApplicants(UUID listingId) {
        return adoptionRequestRepository.findByListingId(listingId).stream()
                .map(detail -> adoptionRequestMapper.toResponse(detail.getRequest(), detail))
                .toList();
    }
}
