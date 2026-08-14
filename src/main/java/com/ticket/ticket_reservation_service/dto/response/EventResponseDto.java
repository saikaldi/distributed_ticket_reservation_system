package com.ticket.ticket_reservation_service.dto.response;

import lombok.*;
import org.hibernate.validator.constraints.UUID;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponseDto {

    private UUID id;
    private String title;
    private String description;
    private OffsetDateTime startTime;
    private String venueName;
    private String venueAddress;
}
