package com.ticket.ticket_reservation_service.controller;

import com.ticket.ticket_reservation_service.dto.response.EventResponseDto;
import com.ticket.ticket_reservation_service.dto.response.SeatResponseDto;
import com.ticket.ticket_reservation_service.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventResponseDto>> getAllEvents(){
        return ResponseEntity.ok(eventService.getAllEvents());

    }
    @GetMapping("/{eventId}/seats")
    public ResponseEntity<List<SeatResponseDto>> getSeatsForEvent(@PathVariable UUID eventId){
        return ResponseEntity.ok(eventService.getSeatsForEvent(eventId));
    }
}
