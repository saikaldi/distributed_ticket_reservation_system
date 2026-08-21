package com.ticket.ticket_reservation_service.repository;

import com.ticket.ticket_reservation_service.entity.Reservation;
import com.ticket.ticket_reservation_service.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    List<Reservation> findByUserId(UUID userId);

    List<Reservation> findByEventIdAndStatusAndExpiresAtAfter(UUID eventId, ReservationStatus status, OffsetDateTime time);

    Optional<Reservation> findByEventIdAndSeatIdAndStatusIn(

            UUID eventId,
            UUID seatId,
            Collection<ReservationStatus> statuses
    );

    // Find all expired reservations in a specific status
    List<Reservation> findAllByStatusAndExpiresAtBefore(ReservationStatus status, OffsetDateTime threshold);
}
