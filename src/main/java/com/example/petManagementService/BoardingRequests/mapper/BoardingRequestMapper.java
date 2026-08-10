package com.example.petManagementService.BoardingRequests.mapper;

import com.example.petManagementService.BoardingRequests.dto.BoardingRequestCreate;
import com.example.petManagementService.BoardingRequests.dto.BoardingRequestResponse;
import com.example.petManagementService.BoardingRequests.entities.BoardingRequests;
import com.example.petManagementService.requests.entities.Request;
import org.springframework.stereotype.Component;

@Component
public class BoardingRequestMapper {

    public BoardingRequests toEntity(BoardingRequestCreate req) {
        BoardingRequests b = new BoardingRequests();
        applyTo(req, b);
        return b;
    }

    public void applyTo(BoardingRequestCreate req, BoardingRequests b) {
        b.setStartDate(req.startDate());
        b.setEndDate(req.endDate());
        b.setSpecialInstructions(req.specialInstructions());
    }

    public BoardingRequestResponse toResponse(BoardingRequests b) {
        Request r = b.getRequest();
        return new BoardingRequestResponse(
                b.getRequestId(), r.getPetId(), r.getCenterId(), r.getRequesterUserId(),
                r.getStatus(), b.getStartDate(), b.getEndDate(),
                b.getSpecialInstructions(), b.getQuotedPrice(),
                b.getCheckedInAt(), b.getCheckedOutAt());
    }
}