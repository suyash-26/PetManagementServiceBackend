package com.example.petManagementService.CareCenter.mapper;

import com.example.petManagementService.CareCenter.dto.CareCenterRequest;
import com.example.petManagementService.CareCenter.dto.CareCenterResponse;
import com.example.petManagementService.CareCenter.entities.CareCenter;
import org.springframework.stereotype.Component;

@Component
public class CareCenterMapper {
    public CareCenter toEntity(CareCenterRequest req){
        CareCenter cc = new CareCenter();
        applyTo(req,cc);
        return cc;
    }

    public void applyTo(CareCenterRequest req, CareCenter cc){
        cc.setName(req.name());
        cc.setDescription(req.description());
        cc.setAddress(req.address());
        cc.setCity(req.city());
        cc.setState(req.state());
        cc.setContactEmail(req.contactEmail());
        cc.setContactPhone(req.contactPhone());
        cc.setLongitude(req.longitude());
        cc.setLatitude(req.latitude());
        cc.setCapacity(req.capacity());
    }

    public CareCenterResponse toResponse(CareCenter c){
        return new CareCenterResponse(
            c.getId() , c.getName(), c.getDescription(), c.getAddress(), c.getCity(),c.getState(),c.getContactEmail(),c.getContactPhone()
                ,c.getLatitude() , c.getLongitude(), c.getCapacity(),c.getStatus(),c.getCreatedBy(),c.getCreatedAt(),c.getUpdatedAt()
        );
    }
}
