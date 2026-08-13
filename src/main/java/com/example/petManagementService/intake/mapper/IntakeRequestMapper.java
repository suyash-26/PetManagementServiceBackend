package com.example.petManagementService.intake.mapper;

import com.example.petManagementService.intake.dto.IntakeRequestCreateRequest;
import com.example.petManagementService.intake.dto.IntakeRequestResponse;
import com.example.petManagementService.intake.entities.IntakeRequest;
import com.example.petManagementService.requests.entities.Request;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IntakeRequestMapper {

    // Two source params: MapStruct matches each target property by name against
    // whichever source object actually has it. requestId comes from detail
    // (IntakeRequest.requestId, its @MapsId-shared PK) rather than request.id, since
    // that's the name that matches — same underlying UUID either way. careCenter comes
    // from request.careCenter — the full entity, matching RequestResponse's shape.
    IntakeRequestResponse toResponse(Request request, IntakeRequest detail);

    // requesterUserId matches the plain Long parameter by name, not a dto property —
    // the create DTO never carries who's asking (that's always the caller, off the
    // JWT, never client-supplied). requestType/status are always INTAKE/PENDING at
    // creation — constants, not something the client chooses. careCenter can't be
    // derived from dto.centerId() (a plain UUID) by a pure mapper — that needs a
    // repository lookup, so it's set separately in IntakeRequestService.
    @Mapping(target = "requestType", constant = "INTAKE")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "careCenter", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignedAdmin", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "decidedAt", ignore = true)
    Request toRequestEntity(Long requesterUserId, IntakeRequestCreateRequest dto);

    // request/requestId are wired separately, after the parent Request is actually
    // saved (its id has to exist first for @MapsId to copy it) — not something a pure
    // DTO->entity mapping can express. handoverCompletedAt is only ever set later, by
    // completing the request, never at creation.
    @Mapping(target = "request", ignore = true)
    @Mapping(target = "requestId", ignore = true)
    @Mapping(target = "handoverCompletedAt", ignore = true)
    IntakeRequest toIntakeEntity(IntakeRequestCreateRequest dto);
}
