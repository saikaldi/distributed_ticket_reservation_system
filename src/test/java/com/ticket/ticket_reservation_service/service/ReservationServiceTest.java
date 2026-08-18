package com.ticket.ticket_reservation_service.service;

import com.ticket.ticket_reservation_service.dto.request.CreateReservationRequestDto;
import com.ticket.ticket_reservation_service.dto.response.ReservationResponseDto;
import com.ticket.ticket_reservation_service.dto.response.TicketResponseDto;
import com.ticket.ticket_reservation_service.entity.*;
import com.ticket.ticket_reservation_service.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SeatLockService seatLockService;

    @InjectMocks
    private ReservationService reservationService;

    @Mock
    private TicketRepository ticketRepository;

    @Test
    @DisplayName("Should successfully create reservation when seat is free")
    void reserveSeat_Success() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreateReservationRequestDto request = CreateReservationRequestDto.builder()
                .eventId(eventId)
                .seatId(seatId)
                .userId(userId)
                .build();

        Event event = Event.builder().id(eventId).title("Concert").build();
        User user = User.builder().id(userId).email("user@mail.com").build();
        Seat seat = Seat.builder().id(seatId).rowNumber(1).seatNumber(1).build();

        Reservation savedReservation = Reservation.builder()
                .id(UUID.randomUUID())
                .event(event)
                .seat(seat)
                .user(user)
                .status(ReservationStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();

        when(seatLockService.acquireLock(eq(eventId), eq(seatId), eq(userId), any(Duration.class))).thenReturn(true);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(seatRepository.findByIdWithPessimisticLock(seatId)).thenReturn(Optional.of(seat));
        when(reservationRepository.findByEventIdAndSeatIdAndStatusIn(eq(eventId), eq(seatId), any())).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        // Act
        ReservationResponseDto response = reservationService.reserveSeat(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getReservationId()).isEqualTo(savedReservation.getId());
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.PENDING);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw exception when Redis lock cannot be acquired")
    void reserveSeat_RedisLockFails_ThrowsException() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreateReservationRequestDto request = CreateReservationRequestDto.builder()
                .eventId(eventId)
                .seatId(seatId)
                .userId(userId)
                .build();

        when(seatLockService.acquireLock(eq(eventId), eq(seatId), eq(userId), any(Duration.class))).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> reservationService.reserveSeat(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Seat is currently being reserved by another user");

        verifyNoInteractions(eventRepository, userRepository, seatRepository, reservationRepository);
    }

    @Test
    @DisplayName("Should release Redis lock and throw exception when seat is already reserved in DB")
    void reserveSeat_AlreadyReservedInDb_ReleasesLockAndThrowsException() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreateReservationRequestDto request = CreateReservationRequestDto.builder()
                .eventId(eventId)
                .seatId(seatId)
                .userId(userId)
                .build();

        Event event = Event.builder().id(eventId).build();
        User user = User.builder().id(userId).build();
        Seat seat = Seat.builder().id(seatId).build();

        Reservation activeReservation = Reservation.builder()
                .id(UUID.randomUUID())
                .status(ReservationStatus.CONFIRMED)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build();

        when(seatLockService.acquireLock(eq(eventId), eq(seatId), eq(userId), any(Duration.class))).thenReturn(true);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(seatRepository.findByIdWithPessimisticLock(seatId)).thenReturn(Optional.of(seat));
        when(reservationRepository.findByEventIdAndSeatIdAndStatusIn(eq(eventId), eq(seatId), any()))
                .thenReturn(Optional.of(activeReservation));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.reserveSeat(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Seat is already reserved or booked for this event");

        verify(seatLockService, times(1)).releaseLock(eventId, seatId, userId);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should successfully confirm reservation and issue ticket")
    void confirmReservation_Success() {
        // Arrange
        UUID reservationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Venue venue = Venue.builder().name("Grand Arena").build();
        Event event = Event.builder().id(eventId).title("Rock Symphony").venue(venue).build();
        User user = User.builder().id(userId).email("listener@test.com").build();
        Seat seat = Seat.builder().id(seatId).rowNumber(2).seatNumber(15).price(BigDecimal.valueOf(100)).build();

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .event(event)
                .seat(seat)
                .user(user)
                .status(ReservationStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .build();

        Ticket savedTicket = Ticket.builder()
                .id(UUID.randomUUID())
                .reservation(reservation)
                .ticketCode("TKT-ABC12345")
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);

        // Act
        TicketResponseDto response = reservationService.confirmReservation(reservationId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTicketNumber()).isEqualTo("TKT-ABC12345");
        assertThat(response.getEventTitle()).isEqualTo("Rock Symphony");
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        verify(seatLockService, times(1)).releaseLock(eventId, seatId, userId);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Should throw exception and set status EXPIRED when reservation has expired")
    void confirmReservation_Expired_ThrowsException() {
        // Arrange
        UUID reservationId = UUID.randomUUID();

        Reservation expiredReservation = Reservation.builder()
                .id(reservationId)
                .status(ReservationStatus.PENDING)
                .expiresAt(OffsetDateTime.now().minusMinutes(1))
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(expiredReservation));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.confirmReservation(reservationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Reservation has expired");

        assertThat(expiredReservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        verify(reservationRepository, times(1)).save(expiredReservation);
        verifyNoInteractions(ticketRepository);
    }

    @Test
    @DisplayName("Should throw exception when reservation status is not PENDING")
    void confirmReservation_InvalidStatus_ThrowsException() {
        // Arrange
        UUID reservationId = UUID.randomUUID();

        Reservation confirmedReservation = Reservation.builder()
                .id(reservationId)
                .status(ReservationStatus.CONFIRMED)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(confirmedReservation));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.confirmReservation(reservationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot confirm reservation in status: CONFIRMED");

        verify(reservationRepository, never()).save(any(Reservation.class));
        verifyNoInteractions(ticketRepository);
    }
}