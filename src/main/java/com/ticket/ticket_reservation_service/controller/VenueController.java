package com.ticket.ticket_reservation_service.controller;

import com.ticket.ticket_reservation_service.dto.request.CreateVenueRequestDto;
import com.ticket.ticket_reservation_service.dto.response.VenueResponseDto;
import com.ticket.ticket_reservation_service.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    // Only administrators can create new venues
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VenueResponseDto> createVenue(@Valid @RequestBody CreateVenueRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(venueService.createVenue(request));
    }

    // Authenticated users can list all venues
    @GetMapping
    public ResponseEntity<List<VenueResponseDto>> getAllVenues() {
        return ResponseEntity.ok(venueService.getAllVenues());
    }

    // Authenticated users can get a specific venue by ID
    @GetMapping("/{venueId}")
    public ResponseEntity<VenueResponseDto> getVenueById(@PathVariable UUID venueId) {
        return ResponseEntity.ok(venueService.getVenueById(venueId));
    }
}