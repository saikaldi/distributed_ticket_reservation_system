package com.ticket.ticket_reservation_service.service;

import com.ticket.ticket_reservation_service.dto.request.CreateReservationRequestDto;
import com.ticket.ticket_reservation_service.dto.response.ReservationResponseDto;
import com.ticket.ticket_reservation_service.dto.response.TicketResponseDto;
import com.ticket.ticket_reservation_service.entity.*;
import com.ticket.ticket_reservation_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private ReservationService reservationService;

    private UUID eventId;
    private UUID seatId;
    private UUID userId;
    private Event sampleEvent;
    private Seat sampleSeat;
    private User sampleUser;
    private CreateReservationRequestDto requestDto;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        seatId = UUID.randomUUID();
        userId = UUID.randomUUID();

        sampleEvent = Event.builder()
                .id(eventId)
                .title("Rock Concert")
                .build();

        sampleSeat = Seat.builder()
                .id(seatId)
                .rowNumber(2)
                .seatNumber(12)
                .price(BigDecimal.valueOf(100.00))
                .build();

        sampleUser = User.builder()
                .id(userId)
                .fullName("John Doe")
                .email("john@example.com")
                .build();

        requestDto = CreateReservationRequestDto.builder()
                .eventId(eventId)
                .seatId(seatId)
                .build();
    }

    @Test
    @DisplayName("reserveSeat: should successfully create reservation when seat is available")
    void reserveSeat_Success() {
        // Arrange
        when(seatLockService.acquireLock(eq(eventId), eq(seatId), eq(userId), any(Duration.class)))
                .thenReturn(true);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(sampleEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(seatRepository.findByIdWithPessimisticLock(seatId)).thenReturn(Optional.of(sampleSeat));
        when(reservationRepository.findByEventIdAndSeatIdAndStatusIn(eq(eventId), eq(seatId), anyList()))
                .thenReturn(Optional.empty());

        Reservation savedReservation = Reservation.builder()
                .id(UUID.randomUUID())
                .event(sampleEvent)
                .seat(sampleSeat)
                .user(sampleUser)
                .status(ReservationStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .createdAt(OffsetDateTime.now())
                .build();

        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        // Act
        ReservationResponseDto response = reservationService.reserveSeat(userId, requestDto);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEventId()).isEqualTo(eventId);
        assertThat(response.getSeatId()).isEqualTo(seatId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.PENDING);

        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("reserveSeat: should throw exception when Redis lock cannot be acquired")
    void reserveSeat_LockFailed_ThrowsException() {
        // Arrange
        when(seatLockService.acquireLock(eq(eventId), eq(seatId), eq(userId), any(Duration.class)))
                .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> reservationService.reserveSeat(userId, requestDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Seat is currently being reserved");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("confirmReservation: should successfully issue ticket and confirm reservation")
    void confirmReservation_Success() {
        // Arrange
        UUID reservationId = UUID.randomUUID();
        Reservation pendingReservation = Reservation.builder()
                .id(reservationId)
                .event(sampleEvent)
                .seat(sampleSeat)
                .user(sampleUser)
                .status(ReservationStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .build();

        Ticket savedTicket = Ticket.builder()
                .id(UUID.randomUUID())
                .reservation(pendingReservation)
                .ticketCode("TKT-12345678")
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);

        // Act
        TicketResponseDto response = reservationService.confirmReservation(reservationId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getReservationId()).isEqualTo(reservationId);
        assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        verify(seatLockService).releaseLock(eventId, seatId, userId);
    }
}