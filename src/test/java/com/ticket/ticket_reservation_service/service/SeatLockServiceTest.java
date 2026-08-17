package com.ticket.ticket_reservation_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatLockServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private SeatLockService seatLockService;

    private UUID eventId;
    private UUID seatId;
    private UUID userId;
    private Duration ttl;
    private String expectedKey;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        seatId = UUID.randomUUID();
        userId = UUID.randomUUID();
        ttl = Duration.ofMinutes(10);
        expectedKey = String.format("lock:event:%s:seat:%s", eventId, seatId);
    }

    @Test
    @DisplayName("acquireLock should return true when lock is successfully acquired")
    void acquireLock_Success_ShouldReturnTrue() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(expectedKey), eq(userId.toString()), eq(ttl)))
                .thenReturn(true);

        // Act
        boolean result = seatLockService.acquireLock(eventId, seatId, userId, ttl);

        // Assert
        assertThat(result).isTrue();
        verify(valueOperations).setIfAbsent(expectedKey, userId.toString(), ttl);
    }

    @Test
    @DisplayName("acquireLock should return false when seat is already locked")
    void acquireLock_AlreadyLocked_ShouldReturnFalse() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(expectedKey), eq(userId.toString()), eq(ttl)))
                .thenReturn(false);

        // Act
        boolean result = seatLockService.acquireLock(eventId, seatId, userId, ttl);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("releaseLock should delete key and return true when requester is the lock owner")
    void releaseLock_WhenOwner_ShouldReturnTrueAndCallDelete() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(userId.toString());
        when(redisTemplate.delete(expectedKey)).thenReturn(true);

        // Act
        boolean result = seatLockService.releaseLock(eventId, seatId, userId);

        // Assert
        assertThat(result).isTrue();
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    @DisplayName("releaseLock should return false and NOT delete key when requester is not the owner")
    void releaseLock_WhenNotOwner_ShouldReturnFalseAndNotDelete() {
        // Arrange
        UUID anotherUserId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(anotherUserId.toString());

        // Act
        boolean result = seatLockService.releaseLock(eventId, seatId, userId);

        // Assert
        assertThat(result).isFalse();
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("isSeatLocked should return true when key exists in Redis")
    void isSeatLocked_WhenKeyExists_ShouldReturnTrue() {
        // Arrange
        when(redisTemplate.hasKey(expectedKey)).thenReturn(true);

        // Act
        boolean result = seatLockService.isSeatLocked(eventId, seatId);

        // Assert
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey(expectedKey);
    }
}