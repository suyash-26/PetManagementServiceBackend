package com.example.petManagementService.AdoptionRequests.mapper;

import com.example.petManagementService.AdoptionRequests.dto.AdoptionRequestCreateRequest;
import com.example.petManagementService.AdoptionRequests.dto.AdoptionRequestResponse;
import com.example.petManagementService.AdoptionRequests.entities.AdoptionRequest;
import com.example.petManagementService.requests.entities.Request;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AdoptionRequestMapper {

    // Two source params: MapStruct matches each target property by name against
    // whichever source object actually has it, same technique IntakeRequestMapper uses.
    AdoptionRequestResponse toResponse(Request request, AdoptionRequest detail);

    // requesterUserId/petId match the plain parameters by name, not a dto property —
    // the caller and the listing's pet are both resolved by the service (off the JWT
    // and the AdoptionListing lookup respectively), never client-supplied. requestType/
    // status are always ADOPTION/PENDING at creation. careCenter can't be derived from
    // the listing's centerId by a pure mapper — that needs a repository lookup, so it's
    // set separately in AdoptionRequestService.
    @Mapping(target = "requestType", constant = "ADOPTION")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "careCenter", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignedAdmin", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "decidedAt", ignore = true)
    Request toRequestEntity(Long requesterUserId, UUID petId, AdoptionRequestCreateRequest dto);

    // request/requestId are wired separately, after the parent Request is actually
    // saved (its id has to exist first for @MapsId to copy it). listingId/adopterUserId
    // are set by the service, not carried on the create DTO. homeVisitStatus is only
    // ever set later, by the home-visit sub-workflow, never at creation.
    @Mapping(target = "request", ignore = true)
    @Mapping(target = "requestId", ignore = true)
    @Mapping(target = "listingId", ignore = true)
    @Mapping(target = "adopterUserId", ignore = true)
    @Mapping(target = "homeVisitStatus", ignore = true)
    AdoptionRequest toDetailEntity(AdoptionRequestCreateRequest dto);
}
