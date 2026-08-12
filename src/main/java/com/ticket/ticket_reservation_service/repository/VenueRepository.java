package com.ticket.ticket_reservation_service.repository;

import com.ticket.ticket_reservation_service.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {
}
