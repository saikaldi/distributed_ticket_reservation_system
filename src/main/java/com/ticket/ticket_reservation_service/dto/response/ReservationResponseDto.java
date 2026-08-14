package com.ticket.ticket_reservation_service.dto.response;

import lombok.*;
import org.hibernate.validator.constraints.UUID;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponseDto {
    private UUID reservationId;
    private UUID eventId;
    private UUID seatId;
    private String status;
    private OffsetDateTime expiresAt;
}
