package com.ticket.ticket_reservation_service.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponseDto {

    private UUID ticketId;
    private UUID reservationId;
    private String ticketNumber;
    private String eventTitle;
    private String venueName;
    private Integer rowNumber;
    private Integer seatNumber;
    private BigDecimal price;
    private String userEmail;
    private OffsetDateTime issuedAt;
}