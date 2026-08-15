package com.ticket.ticket_reservation_service.controller;

import com.ticket.ticket_reservation_service.dto.response.EventResponseDto;
import com.ticket.ticket_reservation_service.dto.response.SeatResponseDto;
import com.ticket.ticket_reservation_service.service.EventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)

public class EventControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;
    @Test
    @DisplayName("GET /api/events should return 200 OK and list of events")
    void getAllEvents_ShouldReturnOk() throws Exception {
        UUID eventId = UUID.randomUUID();
        EventResponseDto dto = EventResponseDto.builder()
                .id(eventId)
                .title("Coldplay Music of the Spheres")
                .venueName("Jashtyk")
                .startTime(OffsetDateTime.now())
                .build();

        given(eventService.getAllEvents()).willReturn(List.of(dto));

        mockMvc.perform(get("/api/events")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(eventId.toString()))
                .andExpect(jsonPath("$[0].title").value("Coldplay Music of the Spheres"))
                .andExpect(jsonPath("$[0].venueName").value("Wembley"));
    }

    @Test
    @DisplayName("GET /api/events/{eventId}/seats should return 200 OK and seats layout")
    void getSeatsForEvent_ShouldReturnOk() throws Exception {
        UUID eventId = UUID.randomUUID();
        SeatResponseDto seatDto = SeatResponseDto.builder()
                .id(UUID.randomUUID())
                .rowNumber(1)
                .seatNumber(1)
                .category("VIP")
                .price(BigDecimal.valueOf(100))
                .isAvailable(true)
                .build();

        given(eventService.getSeatsForEvent(eventId)).willReturn(List.of(seatDto));

        mockMvc.perform(get("/api/events/{eventId}/seats", eventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("VIP"))
                .andExpect(jsonPath("$[0].available").value(true));
    }
}