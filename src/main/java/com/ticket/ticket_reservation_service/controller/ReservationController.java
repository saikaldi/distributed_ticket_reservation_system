package com.ticket.ticket_reservation_service.controller;

import com.ticket.ticket_reservation_service.dto.request.CreateReservationRequestDto;
import com.ticket.ticket_reservation_service.dto.response.ReservationResponseDto;
import com.ticket.ticket_reservation_service.dto.response.TicketResponseDto;
import com.ticket.ticket_reservation_service.entity.User;
import com.ticket.ticket_reservation_service.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponseDto> createReservation(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateReservationRequestDto request
    ) {
        ReservationResponseDto response = reservationService.reserveSeat(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.reserveSeat(currentUser.getId(), request));
    }
    @PostMapping("/{id}/confirm")
    public ResponseEntity<TicketResponseDto> confirmReservation(@PathVariable UUID id) {
        TicketResponseDto response = reservationService.confirmReservation(id);
        return ResponseEntity.ok(response);
    }
}