package com.ticket.ticket_reservation_service.dto.response;

import com.ticket.ticket_reservation_service.entity.Role;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {

    private String token;

    private UUID userId;

    private String email;

    private Role role;
}
