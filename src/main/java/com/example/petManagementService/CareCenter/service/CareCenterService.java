package com.example.petManagementService.CareCenter.service;

import com.example.petManagementService.CareCenter.dto.CareCenterRequest;
import com.example.petManagementService.CareCenter.dto.CareCenterResponse;
import com.example.petManagementService.CareCenter.entities.CareCenter;
import com.example.petManagementService.CareCenter.entities.CenterMember;
import com.example.petManagementService.CareCenter.enums.CenterStatus;
import com.example.petManagementService.CareCenter.enums.MemberRole;
import com.example.petManagementService.CareCenter.exceptions.DuplicateCenterException;
import com.example.petManagementService.CareCenter.exceptions.IllegalTransitionException;
import com.example.petManagementService.CareCenter.exceptions.ResourceNotFoundException;
import com.example.petManagementService.CareCenter.mapper.CareCenterMapper;
import com.example.petManagementService.CareCenter.repository.CareCenterRepository;
import com.example.petManagementService.CareCenter.repository.CenterMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CareCenterService {
    @Autowired
    public CareCenterRepository centerRepo;
    @Autowired
    public CenterMemberRepository memberRepo;
    @Autowired
    public CareCenterMapper mapper;

    private CareCenter loadOrThrow(UUID id){
        return centerRepo.findById(id).orElseThrow(()-> new ResourceNotFoundException("Center_Not_Found"));
    }

   @Transactional
   public CareCenterResponse createCareCenter(CareCenterRequest request, Long currentUserId){
     if(centerRepo.existsByNameIgnoreCaseAndCityIgnoreCase(request.name(), request.city())){
         throw new DuplicateCenterException("CENTER_ALREADY_EXISTS");
     }
     CareCenter center = mapper.toEntity(request);
     center.setCreatedBy(currentUserId);
       CareCenter saved = centerRepo.save(center);

       CenterMember owner = new CenterMember();
       owner.setCenter(saved);
       owner.setUserId(currentUserId);
       owner.setMemberRole(MemberRole.OWNER);
       memberRepo.save(owner);   // these extra code line is for making an initial admin of a center without this there is no administrative for a new center and then we cannot assign also as it doesnt have access

     return mapper.toResponse(saved);
   }

    public CareCenterResponse getById(UUID id) {
        return mapper.toResponse(loadOrThrow(id));
    }

    public List<CareCenterResponse> search(String city, CenterStatus status) {

        List<CareCenter> results;

        if (city != null && status != null) {
            results = centerRepo.findByStatusAndCityIgnoreCase(status, city);
        } else if (city != null) {
            results = centerRepo.findByCityIgnoreCase(city);
        } else if (status != null) {
            results = centerRepo.findByStatus(status);
        } else {
            results = centerRepo.findAll();
        }

        return results.stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public CareCenterResponse updateCareCenter(UUID id, CareCenterRequest req){
         CareCenter center = loadOrThrow(id);
         mapper.applyTo(req,center);
         return mapper.toResponse(center);
    }

    private boolean canTransition(CenterStatus from, CenterStatus to) {
        return switch (from) {
            case PENDING, SUSPENDED -> to == CenterStatus.ACTIVE;
            case ACTIVE    -> to == CenterStatus.SUSPENDED;
        };
    }

    @Transactional
    public CareCenterResponse updateStatus(UUID id, CenterStatus newStatus) {

        CareCenter center = loadOrThrow(id);

        if (!canTransition(center.getStatus(), newStatus)) {
            throw new IllegalTransitionException("ILLEGAL_TRANSITION");
        }

        center.setStatus(newStatus); // no apply to as it is not handled in dto status cannot be changed from request thats why and then we save it
        return mapper.toResponse(centerRepo.save(center));
    }

}
