package com.ticket.ticket_reservation_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash",nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String full_name;

    @Column(nullable = false)
    private String role;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

}
