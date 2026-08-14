package com.ticket.ticket_reservation_service.repository;

import com.ticket.ticket_reservation_service.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    List<Reservation> findByUserId(UUID userId);

    List<Reservation> findByEventIdAndStatusAndExpiresAtAfter(UUID eventId, String status, OffsetDateTime now);
}
