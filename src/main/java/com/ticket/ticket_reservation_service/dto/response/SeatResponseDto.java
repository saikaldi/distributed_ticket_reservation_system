package com.ticket.ticket_reservation_service.dto.response;

import lombok.*;
import org.hibernate.validator.constraints.UUID;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatResponseDto {
    private UUID id;

    private Integer rowNumber;

    private Integer seatNumber;

    private String category;

    private BigDecimal price;

    private boolean isAvailable;
}
