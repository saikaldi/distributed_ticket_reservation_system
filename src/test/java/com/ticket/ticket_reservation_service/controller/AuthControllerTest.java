package com.ticket.ticket_reservation_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.ticket_reservation_service.dto.request.AuthRequestDto;
import com.ticket.ticket_reservation_service.dto.request.RegisterRequestDto;
import com.ticket.ticket_reservation_service.dto.response.AuthResponseDto;
import com.ticket.ticket_reservation_service.security.JwtAuthenticationFilter;
import com.ticket.ticket_reservation_service.security.JwtService;
import com.ticket.ticket_reservation_service.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(SpringExtension.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /api/v1/auth/register: should return 201 Created and token")
    void register_ValidRequest_ReturnsCreated() throws Exception {
        // Prepare request and response DTOs
        RegisterRequestDto request = RegisterRequestDto.builder()
                .fullName("Alice Johnson")
                .email("alice@example.com")
                .password("securePassword123")
                .build();

        AuthResponseDto response = AuthResponseDto.builder()
                .token("mocked.jwt.token")
                .build();

        when(authService.register(any(RegisterRequestDto.class))).thenReturn(response);

        // Perform POST request and assert response
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mocked.jwt.token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login: should return 200 OK and token")
    void login_ValidRequest_ReturnsOk() throws Exception {
        // Prepare request and response DTOs
        AuthRequestDto request = AuthRequestDto.builder()
                .email("alice@example.com")
                .password("securePassword123")
                .build();

        AuthResponseDto response = AuthResponseDto.builder()
                .token("mocked.jwt.token")
                .build();

        when(authService.login(any(AuthRequestDto.class))).thenReturn(response);

        // Perform POST request and assert response
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked.jwt.token"));
    }
}