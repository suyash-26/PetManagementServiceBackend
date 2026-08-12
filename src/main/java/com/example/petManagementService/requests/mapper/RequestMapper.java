package com.example.petManagementService.requests.mapper;

import com.example.petManagementService.requests.dto.RequestResponse;
import com.example.petManagementService.requests.entities.Request;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    // centerId now comes from the careCenter relationship (Request has no raw centerId
    // column anymore) — everything else still lines up by name.
    @Mapping(target = "careCenter", source = "careCenter")
    RequestResponse toResponse(Request request);
}
