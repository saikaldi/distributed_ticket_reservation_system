package com.ticket.ticket_reservation_service.service;

import com.ticket.ticket_reservation_service.dto.response.EventResponseDto;
import com.ticket.ticket_reservation_service.dto.response.SeatResponseDto;
import com.ticket.ticket_reservation_service.entity.Event;
import com.ticket.ticket_reservation_service.entity.Seat;
import com.ticket.ticket_reservation_service.repository.EventRepository;
import com.ticket.ticket_reservation_service.repository.ReservationRepository;
import com.ticket.ticket_reservation_service.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public List<EventResponseDto> getAllEvents(){
        return eventRepository.findAll().stream().map(this::mapToEventResponseDto).collect(Collectors.toList());
    }

    public List<SeatResponseDto> getSeatsForEvent(UUID eventId){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        List<Seat> allSeats = seatRepository.findByVenueId(event.getVenue().getId());

        Set<UUID> reservedSeatIds = reservationRepository.findByEventIdAndStatusAndExpiresAtAfter(eventId, "PENDING", OffsetDateTime.now())
                .stream().map(reservation -> reservation.getSeat().getId()).collect(Collectors.toSet());

        return allSeats.stream()
                .map(seat -> SeatResponseDto.builder()
                        .id(seat.getId())
                        .rowNumber(seat.getRowNumber())
                        .seatNumber(seat.getSeatNumber())
                        .category(seat.getCategory())
                        .price(seat.getPrice())
                        .isAvailable(!reservedSeatIds.contains(seat.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    private EventResponseDto mapToEventResponseDto(Event event) {
        return EventResponseDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startTime(event.getStartTime())
                .venueName(event.getVenue().getName())
                .venueAddress(event.getVenue().getAddress())
                .build();
    }
}

