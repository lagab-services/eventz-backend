package com.lagab.eventz.app.interfaces.web.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lagab.eventz.app.domain.auth.service.ApiKeyService;
import com.lagab.eventz.app.domain.auth.service.JwtService;
import com.lagab.eventz.app.domain.order.dto.OrderResponse;
import com.lagab.eventz.app.domain.order.dto.TrackOrderRequest;
import com.lagab.eventz.app.domain.order.model.OrderStatus;
import com.lagab.eventz.app.domain.order.service.GuestOrderService;
import com.lagab.eventz.app.domain.user.repository.UserRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GuestOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class GuestOrderControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    GuestOrderService guestOrderService;

    @Test
    @DisplayName("should track order by number and email")
    void trackOrder_success() throws Exception {
        OrderResponse response = new OrderResponse(
                1L,
                "ORD-123",
                OrderStatus.PAID,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                LocalDateTime.now(),
                List.of(),
                "My Event",
                LocalDateTime.now().plusDays(1),
                "Paris",
                LocalDateTime.now().plusMinutes(5),
                null
        );
        when(guestOrderService.trackOrder(any(TrackOrderRequest.class))).thenReturn(response);

        TrackOrderRequest req = new TrackOrderRequest("ORD-123", "john.doe@example.com");

        mockMvc.perform(post("/api/guest/orders/track")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should fail validation for invalid email")
    void trackOrder_validationFail() throws Exception {
        // invalid email should trigger 400
        TrackOrderRequest req = new TrackOrderRequest("ORD-123", "not-an-email");
        mockMvc.perform(post("/api/guest/orders/track")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should download ticket as PDF")
    void downloadTicket_success() throws Exception {
        byte[] pdf = "%PDF-1.4".getBytes();
        when(guestOrderService.downloadTicket("ORD-123", 10L)).thenReturn(pdf);

        mockMvc.perform(get("/api/guest/orders/ORD-123/attendees/10/ticket"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_PDF))
               .andExpect(header().string("Content-Disposition", "attachment; filename=ticket-10.pdf"));
    }

    @Test
    @DisplayName("should download ticket by event and email")
    void downloadTicketByEventAndEmail_success() throws Exception {
        byte[] pdf = "%PDF-1.4".getBytes();
        when(guestOrderService.downloadTicketByEventAndEmail(5L, "john.doe@example.com")).thenReturn(pdf);

        mockMvc.perform(get("/api/guest/orders/events/5/ticket")
                       .param("email", "john.doe@example.com"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_PDF))
               .andExpect(header().string("Content-Disposition", "attachment; filename=ticket-event-5.pdf"));
    }
}
