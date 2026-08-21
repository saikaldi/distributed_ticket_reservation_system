package com.ticket.ticket_reservation_service.service;

import com.ticket.ticket_reservation_service.dto.request.CreateEventRequestDto;
import com.ticket.ticket_reservation_service.dto.response.EventResponseDto;
import com.ticket.ticket_reservation_service.dto.response.SeatResponseDto;
import com.ticket.ticket_reservation_service.entity.Event;
import com.ticket.ticket_reservation_service.entity.ReservationStatus;
import com.ticket.ticket_reservation_service.entity.Seat;
import com.ticket.ticket_reservation_service.entity.Venue;
import com.ticket.ticket_reservation_service.repository.EventRepository;
import com.ticket.ticket_reservation_service.repository.ReservationRepository;
import com.ticket.ticket_reservation_service.repository.SeatRepository;
import com.ticket.ticket_reservation_service.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final VenueRepository venueRepository;

    @Transactional
    public EventResponseDto createEvent(CreateEventRequestDto request) {
        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new IllegalArgumentException("Venue not found with ID: " + request.getVenueId()));

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .venue(venue)
                .startTime(request.getStartTime())
                .build();

        Event savedEvent = eventRepository.save(event);
        log.info("Event created successfully: eventId={}, title={}", savedEvent.getId(), savedEvent.getTitle());

        return mapToEventResponseDto(savedEvent);
    }

    @Transactional(readOnly = true)
    public List<EventResponseDto> getAllEvents(){
        return eventRepository.findAll().stream().map(this::mapToEventResponseDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventResponseDto getEventById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        return mapToEventResponseDto(event);
    }

    public List<SeatResponseDto> getSeatsForEvent(UUID eventId){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        List<Seat> allSeats = seatRepository.findByVenueId(event.getVenue().getId());

        Set<UUID> reservedSeatIds = reservationRepository
                .findByEventIdAndStatusAndExpiresAtAfter(eventId, ReservationStatus.PENDING, OffsetDateTime.now())
                .stream()
                .map(reservation -> reservation.getSeat().getId())
                .collect(Collectors.toSet());
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
                .venueId(event.getVenue() != null ? event.getVenue().getId() : null)
                .venueName(event.getVenue() != null ? event.getVenue().getName() : null)
                .startTime(event.getStartTime())
                .createdAt(event.getCreatedAt())
                .build();
    }
    }


