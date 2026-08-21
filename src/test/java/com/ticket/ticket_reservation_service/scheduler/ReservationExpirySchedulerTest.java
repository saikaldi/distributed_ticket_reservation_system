package com.ticket.ticket_reservation_service.scheduler;

import com.ticket.ticket_reservation_service.entity.Event;
import com.ticket.ticket_reservation_service.entity.Reservation;
import com.ticket.ticket_reservation_service.entity.ReservationStatus;
import com.ticket.ticket_reservation_service.entity.Seat;
import com.ticket.ticket_reservation_service.entity.User;
import com.ticket.ticket_reservation_service.repository.ReservationRepository;
import com.ticket.ticket_reservation_service.service.SeatLockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationExpirySchedulerTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SeatLockService seatLockService;

    @InjectMocks
    private ReservationExpiryScheduler reservationExpiryScheduler;

    @Test
    @DisplayName("expirePendingReservations: should mark expired reservations as EXPIRED and release lock")
    void expirePendingReservations_Success() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Event event = Event.builder().id(eventId).build();
        Seat seat = Seat.builder().id(seatId).build();
        User user = User.builder().id(userId).build();

        Reservation expiredReservation = Reservation.builder()
                .id(UUID.randomUUID())
                .event(event)
                .seat(seat)
                .user(user)
                .status(ReservationStatus.PENDING)
                .expiresAt(OffsetDateTime.now().minusMinutes(5))
                .build();

        when(reservationRepository.findAllByStatusAndExpiresAtBefore(eq(ReservationStatus.PENDING), any(OffsetDateTime.class)))
                .thenReturn(List.of(expiredReservation));

        // Act
        reservationExpiryScheduler.expirePendingReservations();

        // Assert
        assertThat(expiredReservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        verify(seatLockService).releaseLock(eventId, seatId, userId);
        verify(reservationRepository).saveAll(List.of(expiredReservation));
    }
}