package com.ticket.ticket_reservation_service.controller;


import com.ticket.ticket_reservation_service.dto.request.CreateReservationRequestDto;
import com.ticket.ticket_reservation_service.dto.response.ReservationResponseDto;
import com.ticket.ticket_reservation_service.dto.response.TicketResponseDto;
import com.ticket.ticket_reservation_service.entity.ReservationStatus;
import com.ticket.ticket_reservation_service.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import tools.jackson.databind.ObjectMapper;


@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService reservationService;

    @Test
    @DisplayName("POST /api/v1/reservations - should return 201 Created")
    void createReservation_Success() throws Exception {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();

        CreateReservationRequestDto request = CreateReservationRequestDto.builder()
                .eventId(eventId)
                .seatId(seatId)
                .build();

        ReservationResponseDto response = ReservationResponseDto.builder()
                .reservationId(reservationId)
                .eventId(eventId)
                .seatId(seatId)
                .userId(userId)
                .status(ReservationStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .createdAt(OffsetDateTime.now())
                .build();

        when(reservationService.reserveSeat(any(), any(CreateReservationRequestDto.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(reservationId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/reservations/{id}/confirm - should return 200 OK with Ticket info")
    void confirmReservation_Success() throws Exception {
        // Arrange
        UUID reservationId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        TicketResponseDto response = TicketResponseDto.builder()
                .ticketId(ticketId)
                .reservationId(reservationId)
                .ticketNumber("TKT-12345678")
                .eventTitle("Rock Symphony")
                .venueName("Grand Arena")
                .rowNumber(1)
                .seatNumber(10)
                .price(BigDecimal.valueOf(150.00))
                .userEmail("fan@test.com")
                .issuedAt(OffsetDateTime.now())
                .build();

        when(reservationService.confirmReservation(any(), eq(reservationId))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/reservations/{id}/confirm", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(ticketId.toString()))
                .andExpect(jsonPath("$.ticketNumber").value("TKT-12345678"))
                .andExpect(jsonPath("$.eventTitle").value("Rock Symphony"));
    }
}