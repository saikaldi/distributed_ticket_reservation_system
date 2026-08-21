package com.ticket.ticket_reservation_service.service;

import com.ticket.ticket_reservation_service.dto.request.CreateReservationRequestDto;
import com.ticket.ticket_reservation_service.dto.response.ReservationResponseDto;
import com.ticket.ticket_reservation_service.dto.response.TicketResponseDto;
import com.ticket.ticket_reservation_service.entity.*;
import com.ticket.ticket_reservation_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final Duration LOCK_TTL = Duration.ofMinutes(10);
    private static final long RESERVATION_EXPIRATION_MINUTES = 10;

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final SeatLockService seatLockService;
    private final TicketRepository ticketRepository;


    @Transactional
    public ReservationResponseDto reserveSeat(UUID userId, CreateReservationRequestDto request) {

        boolean lockAcquired = seatLockService.acquireLock(
                request.getEventId(),
                request.getSeatId(),
                userId,
                LOCK_TTL
        );

        if (!lockAcquired) {
            throw new IllegalStateException("Seat is currently being reserved by another user");
        }

        try {
            // Validate event existence
            Event event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + request.getEventId()));

            // Validate user existence
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

            // Acquire database row-level pessimistic write lock (SELECT ... FOR UPDATE)
            Seat seat = seatRepository.findByIdWithPessimisticLock(request.getSeatId())
                    .orElseThrow(() -> new IllegalArgumentException("Seat not found with ID: " + request.getSeatId()));

            // Check if there is already an active reservation in DB
            Optional<Reservation> existingActiveReservation = reservationRepository.findByEventIdAndSeatIdAndStatusIn(
                    request.getEventId(),
                    request.getSeatId(),
                    List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
            );

            if (existingActiveReservation.isPresent()) {
                Reservation existing = existingActiveReservation.get();
                // If reservation is CONFIRMED or still valid PENDING -> reject
                if (existing.getStatus() == ReservationStatus.CONFIRMED || existing.getExpiresAt().isAfter(OffsetDateTime.now())) {
                    throw new IllegalStateException("Seat is already reserved or booked for this event");
                }
            }

            // Create new PENDING reservation
            OffsetDateTime now = OffsetDateTime.now();
            Reservation reservation = Reservation.builder()
                    .event(event)
                    .seat(seat)
                    .user(user)
                    .status(ReservationStatus.PENDING)
                    .expiresAt(now.plusMinutes(RESERVATION_EXPIRATION_MINUTES))
                    .build();

            Reservation savedReservation = reservationRepository.save(reservation);
            log.info("Reservation created successfully: reservationId={}, userId={}, seatId={}",
                    savedReservation.getId(), user.getId(), seat.getId());

            return ReservationResponseDto.builder()
                    .reservationId(savedReservation.getId())
                    .eventId(event.getId())
                    .seatId(seat.getId())
                    .userId(user.getId())
                    .status(savedReservation.getStatus())
                    .expiresAt(savedReservation.getExpiresAt())
                    .createdAt(savedReservation.getCreatedAt())
                    .build();

        } catch (Exception ex) {
            // Release Redis lock if DB checks fail or an exception occurs
            seatLockService.releaseLock(request.getEventId(), request.getSeatId(), userId);
            throw ex;
        }
    }
    @Transactional
    public TicketResponseDto confirmReservation(UUID userId, UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with ID: " + reservationId));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new IllegalStateException("You do not have permission to confirm this reservation");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Cannot confirm reservation in status: " + reservation.getStatus());
        }

        if (reservation.getExpiresAt().isBefore(OffsetDateTime.now())) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            throw new IllegalStateException("Reservation has expired. Please reserve the seat again.");
        }

        // Update reservation status to CONFIRMED
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        // Issue ticket
        String ticketNumber = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Ticket ticket = Ticket.builder()
                .reservation(reservation)
                .ticketCode(ticketNumber)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket issued successfully: ticketNumber={}, reservationId={}", ticketNumber, reservationId);

        // Release temporary Redis lock (permanent state now stored in PostgreSQL)
        seatLockService.releaseLock(
                reservation.getEvent().getId(),
                reservation.getSeat().getId(),
                reservation.getUser().getId()
        );

        return TicketResponseDto.builder()
                .ticketId(savedTicket.getId())
                .reservationId(reservation.getId())
                .ticketNumber(savedTicket.getTicketCode())
                .eventTitle(reservation.getEvent().getTitle())
                .venueName(reservation.getEvent().getVenue() != null ? reservation.getEvent().getVenue().getName() : null)
                .rowNumber(reservation.getSeat().getRowNumber())
                .seatNumber(reservation.getSeat().getSeatNumber())
                .price(reservation.getSeat().getPrice())
                .userEmail(reservation.getUser().getEmail())
                .issuedAt(OffsetDateTime.now())
                .build();
    }
}