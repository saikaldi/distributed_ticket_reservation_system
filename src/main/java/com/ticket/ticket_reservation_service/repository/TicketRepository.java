package com.ticket.ticket_reservation_service.repository;

import com.ticket.ticket_reservation_service.entity.Ticket;
import org.hibernate.validator.constraints.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Optional<Ticket> findByTicketCode(String ticketCode);
}
