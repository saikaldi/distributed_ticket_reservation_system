package com.ticket.ticket_reservation_service.service;

import com.ticket.ticket_reservation_service.dto.request.CreateReservationRequestDto;
import com.ticket.ticket_reservation_service.dto.response.ReservationResponseDto;
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

    @Transactional
    public ReservationResponseDto reserveSeat(CreateReservationRequestDto request) {

        boolean lockAcquired = seatLockService.acquireLock(
                request.getEventId(),
                request.getSeatId(),
                request.getUserId(),
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
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + request.getUserId()));

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
            seatLockService.releaseLock(request.getEventId(), request.getSeatId(), request.getUserId());
            throw ex;
        }
    }
}