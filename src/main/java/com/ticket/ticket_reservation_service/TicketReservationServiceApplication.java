package com.ticket.ticket_reservation_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TicketReservationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketReservationServiceApplication.class, args);
	}

}
