package com.ticket.ticket_reservation_service.scheduler;

import com.ticket.ticket_reservation_service.entity.Reservation;
import com.ticket.ticket_reservation_service.entity.ReservationStatus;
import com.ticket.ticket_reservation_service.repository.ReservationRepository;
import com.ticket.ticket_reservation_service.service.SeatLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpiryScheduler {

    private final ReservationRepository reservationRepository;
    private final SeatLockService seatLockService;

    // Run every 60 seconds (60000 ms) with an initial delay of 10 seconds
    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    @Transactional
    public void expirePendingReservations() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Reservation> expiredReservations = reservationRepository.findAllByStatusAndExpiresAtBefore(
                ReservationStatus.PENDING,
                now
        );

        if (expiredReservations.isEmpty()) {
            return;
        }

        log.info("Found {} expired pending reservations to process", expiredReservations.size());

        for (Reservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.EXPIRED);

            // Release Redis lock in case it is still active
            seatLockService.releaseLock(
                    reservation.getEvent().getId(),
                    reservation.getSeat().getId(),
                    reservation.getUser().getId()
            );

            log.info("Reservation expired and seat lock released: reservationId={}, seatId={}",
                    reservation.getId(), reservation.getSeat().getId());
        }

        reservationRepository.saveAll(expiredReservations);
    }
}