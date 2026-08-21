package com.ticket.ticket_reservation_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDto {

    private UUID id;
    private String title;
    private String description;
    private UUID venueId;
    private String venueName;
    private OffsetDateTime startTime;
    private OffsetDateTime createdAt;
}