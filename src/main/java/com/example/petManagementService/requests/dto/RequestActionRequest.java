package com.example.petManagementService.requests.dto;

// Optional body for reject/cancel — a remark explaining the decision. Nothing requires
// it (empty body is fine), so it's a plain nullable-field record, not validated @NotBlank.
public record RequestActionRequest(String notes) {
}
