package com.ticket.ticket_reservation_service.service;

import com.ticket.ticket_reservation_service.dto.request.AuthRequestDto;
import com.ticket.ticket_reservation_service.dto.request.RegisterRequestDto;
import com.ticket.ticket_reservation_service.dto.response.AuthResponseDto;
import com.ticket.ticket_reservation_service.entity.Role;
import com.ticket.ticket_reservation_service.entity.User;
import com.ticket.ticket_reservation_service.repository.UserRepository;
import com.ticket.ticket_reservation_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ticket.ticket_reservation_service.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request){
        // Check if email already exists
        if(userRepository.findByEmail(request.getEmail()).isPresent()){
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(request.getPassword())
                .role(Role.USER)
                .build();

        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);

        return AuthResponseDto.builder().token(jwtToken).build();

    }

    public AuthResponseDto login(AuthRequestDto request) {
        // Authenticate user credentials via AuthenticationManager
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Load user details from the database
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        // Generate and return JWT access token
        String jwtToken = jwtService.generateToken(user);

        return AuthResponseDto.builder()
                .token(jwtToken)
                .build();
    }
}
