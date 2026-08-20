package com.ticket.ticket_reservation_service.service;

import com.ticket.ticket_reservation_service.dto.request.AuthRequestDto;
import com.ticket.ticket_reservation_service.dto.request.RegisterRequestDto;
import com.ticket.ticket_reservation_service.dto.response.AuthResponseDto;
import com.ticket.ticket_reservation_service.entity.Role;
import com.ticket.ticket_reservation_service.entity.User;
import com.ticket.ticket_reservation_service.exception.ResourceNotFoundException;
import com.ticket.ticket_reservation_service.repository.UserRepository;
import com.ticket.ticket_reservation_service.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("John Doe")
                .email("john@example.com")
                .passwordHash("encoded_secret_hash")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("register: should successfully save user and return JWT token")
    void register_Success() {
        // Arrange
        RegisterRequestDto request = RegisterRequestDto.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .password("rawPassword123")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_secret_hash");
        when(jwtService.generateToken(any(User.class))).thenReturn("mocked.jwt.token");

        // Act
        AuthResponseDto response = authService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(any(User.class));
    }

    @Test
    @DisplayName("register: should throw IllegalArgumentException when email is already taken")
    void register_DuplicateEmail_ThrowsException() {
        // Arrange
        RegisterRequestDto request = RegisterRequestDto.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .password("rawPassword123")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(sampleUser));

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    @DisplayName("login: should authenticate and return fresh JWT token")
    void login_Success() {
        // Arrange
        AuthRequestDto request = AuthRequestDto.builder()
                .email("john@example.com")
                .password("rawPassword123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(sampleUser)).thenReturn("fresh.jwt.token");

        // Act
        AuthResponseDto response = authService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("fresh.jwt.token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login: should throw ResourceNotFoundException when user is not found in database")
    void login_UserNotFound_ThrowsException() {
        // Arrange
        AuthRequestDto request = AuthRequestDto.builder()
                .email("unknown@example.com")
                .password("password")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}