package com.ticket.ticket_reservation_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVenueRequestDto {

    @NotBlank(message = "Venue name is required")
    private String name;

    @NotBlank(message = "Venue city is required")
    private String city;

    @NotBlank(message = "Venue address is required")
    private String address;

    @NotNull(message = "Total capacity is required")
    @Positive(message = "Capacity must be greater than zero")
    private Integer totalCapacity;
}