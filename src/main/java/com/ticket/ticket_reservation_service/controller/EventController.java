package com.ticket.ticket_reservation_service.controller;

import com.ticket.ticket_reservation_service.dto.request.CreateEventRequestDto;
import com.ticket.ticket_reservation_service.dto.response.EventResponseDto;
import com.ticket.ticket_reservation_service.dto.response.SeatResponseDto;
import com.ticket.ticket_reservation_service.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    // Only administrators can create events
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponseDto> createEvent(@Valid @RequestBody CreateEventRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }

    @GetMapping
    public ResponseEntity<List<EventResponseDto>> getAllEvents(){
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    // Authenticated users can get event details by ID
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponseDto> getEventById(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getEventById(eventId));
    }

    @GetMapping("/{eventId}/seats")
    public ResponseEntity<List<SeatResponseDto>> getSeatsForEvent(@PathVariable UUID eventId){
        return ResponseEntity.ok(eventService.getSeatsForEvent(eventId));
    }
}
