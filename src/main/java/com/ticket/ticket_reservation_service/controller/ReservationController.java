package com.ticket.ticket_reservation_service.controller;

import com.ticket.ticket_reservation_service.dto.request.CreateReservationRequestDto;
import com.ticket.ticket_reservation_service.dto.response.ReservationResponseDto;
import com.ticket.ticket_reservation_service.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponseDto> createReservation(
            @Valid @RequestBody CreateReservationRequestDto request
    ) {
        ReservationResponseDto response = reservationService.reserveSeat(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}