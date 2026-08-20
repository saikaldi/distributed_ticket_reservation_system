package com.ticket.ticket_reservation_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReservationRequestDto {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotNull(message = "Seat ID is required")
    private UUID seatId;


}