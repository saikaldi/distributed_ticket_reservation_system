package com.ticket.ticket_reservation_service.dto.response;

import com.ticket.ticket_reservation_service.entity.ReservationStatus;
import lombok.*;
import java.util.UUID;

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
    private UUID userId;
    private ReservationStatus status;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
}
