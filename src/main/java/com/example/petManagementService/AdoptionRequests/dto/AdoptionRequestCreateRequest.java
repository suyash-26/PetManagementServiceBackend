package com.example.petManagementService.AdoptionRequests.dto;

// listingId is not here — it comes from the path (/listings/{id}/requests), not the
// body. Both fields are optional: an applicant may apply with just a bare click.
public record AdoptionRequestCreateRequest(
        String message,
        String screeningAnswers
) {
}
