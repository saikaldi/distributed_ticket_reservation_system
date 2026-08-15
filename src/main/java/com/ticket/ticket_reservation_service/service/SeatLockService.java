package com.ticket.ticket_reservation_service.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockService {

    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_KEY_PREFIX = "lock:event:%s:seat:%s";

    public boolean aqquireLock(UUID eventId, UUID seatId, UUID userId, Duration ttl) {
        String lockKey = buildLockKey(eventId, seatId);
        String lockValue = userId.toString();


        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, ttl);

        boolean acquired = Boolean.TRUE.equals(success);
        if (acquired) {
            log.info("Seat lock acquired: eventId={}, seatId={}, userId={}, ttl={}s",
                    eventId, seatId, userId, ttl.getSeconds());
        } else {
            log.warn("Failed to acquire seat lock (already locked): eventId={}, seatId={}, userId={}",
                    eventId, seatId, userId);
        }

        return acquired;
    }

    private String buildLockKey(UUID eventId, UUID seatId) {
        return String.format(LOCK_KEY_PREFIX, eventId, seatId);
    }

    public boolean releaseLock(UUID eventId, UUID seatId, UUID userId) {
        String lockKey = buildLockKey(eventId, seatId);
        String currentValue = redisTemplate.opsForValue().get(lockKey);

        // Security check, ensure only the lock owner can release it
        if (currentValue != null && currentValue.equals(userId.toString())) {
            Boolean deleted = redisTemplate.delete(lockKey);
            log.info("Seat lock released: eventId={}, seatId={}, userId={}", eventId, seatId, userId);
            return Boolean.TRUE.equals(deleted);
        }

        log.warn("Cannot release seat lock: lock not found or owned by different user. key={}, userId={}",
                lockKey, userId);
        return false;
    }


    public boolean isSeatLocked(UUID eventId, UUID seatId) {
        String lockKey = buildLockKey(eventId, seatId);
        Boolean hasKey = redisTemplate.hasKey(lockKey);
        return Boolean.TRUE.equals(hasKey);
    }

}
