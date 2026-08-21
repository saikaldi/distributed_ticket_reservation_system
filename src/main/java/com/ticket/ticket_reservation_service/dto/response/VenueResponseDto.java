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
public class VenueResponseDto {

    private UUID id;
    private String name;
    private String city;
    private String address;
    private Integer totalCapacity;
    private OffsetDateTime createdAt;
}