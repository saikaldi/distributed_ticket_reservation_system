package com.ticket.ticket_reservation_service.service;

import com.ticket.ticket_reservation_service.dto.response.EventResponseDto;
import com.ticket.ticket_reservation_service.dto.response.SeatResponseDto;
import com.ticket.ticket_reservation_service.entity.*;
import com.ticket.ticket_reservation_service.repository.EventRepository;
import com.ticket.ticket_reservation_service.repository.ReservationRepository;
import com.ticket.ticket_reservation_service.repository.SeatRepository;
import com.ticket.ticket_reservation_service.entity.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    @DisplayName("Should correctly calculate seat availability for event")
    void getAllEvents_ShouldReturnMappedDtos(){
        // Arrange
        Venue venue = Venue.builder()
                .name("Grand Arena")
                .address("12 Togolok Moldo")
                .build();

        Event event = Event.builder()
                .id(UUID.randomUUID())
                .title("Rock Symphony")
                .description("Live orchestra")
                .startTime(OffsetDateTime.now().plusDays(5))
                .venue(venue)
                .build();

        when(eventRepository.findAll()).thenReturn(List.of(event));

        // Act
        List<EventResponseDto> result = eventService.getAllEvents();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Rock Symphony");
        assertThat(result.get(0).getVenueName()).isEqualTo("Grand Arena");

    }

    @Test
    @DisplayName("Should correctly calculate seat availability for event")
    void getSeatsForEvent_ShouldCalculateAvailability() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();

        Venue venue = Venue.builder().id(venueId).name("Main Hall").build();
        Event event = Event.builder().id(eventId).venue(venue).build();

        UUID seat1Id = UUID.randomUUID();
        UUID seat2Id = UUID.randomUUID();

        Seat seat1 = Seat.builder().id(seat1Id).rowNumber(1).seatNumber(1).price(BigDecimal.valueOf(50)).build();
        Seat seat2 = Seat.builder().id(seat2Id).rowNumber(1).seatNumber(2).price(BigDecimal.valueOf(50)).build();

        // Active reservation exists only for seat1
        Reservation activeReservation = Reservation.builder()
                .id(UUID.randomUUID())
                .seat(seat1)
                .status(ReservationStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(seatRepository.findByVenueId(venueId)).thenReturn(List.of(seat1, seat2));
        when(reservationRepository.findByEventIdAndStatusAndExpiresAtAfter(eq(eventId), eq(ReservationStatus.PENDING), any()))
                .thenReturn(List.of(activeReservation));

        // Act
        List<SeatResponseDto> result = eventService.getSeatsForEvent(eventId);

        // Assert
        assertThat(result).hasSize(2);

        SeatResponseDto seat1Dto = result.stream().filter(s -> s.getId().equals(seat1Id)).findFirst().orElseThrow();
        SeatResponseDto seat2Dto = result.stream().filter(s -> s.getId().equals(seat2Id)).findFirst().orElseThrow();

        assertThat(seat1Dto.isAvailable()).isFalse(); // Booked!
        assertThat(seat2Dto.isAvailable()).isTrue();  // Free!
    }
}


