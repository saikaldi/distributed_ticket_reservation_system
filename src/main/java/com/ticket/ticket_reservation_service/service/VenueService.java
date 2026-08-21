package com.ticket.ticket_reservation_service.service;

import com.ticket.ticket_reservation_service.dto.request.CreateVenueRequestDto;
import com.ticket.ticket_reservation_service.dto.response.VenueResponseDto;
import com.ticket.ticket_reservation_service.entity.Venue;
import com.ticket.ticket_reservation_service.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;

    @Transactional
    public VenueResponseDto createVenue(CreateVenueRequestDto request) {
        Venue venue = Venue.builder()
                .name(request.getName())
                .address(request.getAddress())
                .address(request.getAddress())
                .totalCapacity(request.getTotalCapacity())
                .build();

        Venue savedVenue = venueRepository.save(venue);
        log.info("Venue created successfully: venueId={}, name={}", savedVenue.getId(), savedVenue.getName());

        return mapToResponseDto(savedVenue);
    }

    @Transactional(readOnly = true)
    public List<VenueResponseDto> getAllVenues() {
        return venueRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public VenueResponseDto getVenueById(UUID venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found with ID: " + venueId));
        return mapToResponseDto(venue);
    }

    private VenueResponseDto mapToResponseDto(Venue venue) {
        return VenueResponseDto.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .address(venue.getAddress())
                .totalCapacity(venue.getTotalCapacity())
                .createdAt(venue.getCreatedAt())
                .build();
    }
}