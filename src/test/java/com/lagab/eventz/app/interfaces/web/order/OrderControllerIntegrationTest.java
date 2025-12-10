package com.lagab.eventz.app.interfaces.web.order;

import com.lagab.eventz.app.config.WithMockCustomUser;
import com.lagab.eventz.app.domain.auth.service.ApiKeyService;
import com.lagab.eventz.app.domain.auth.service.JwtService;
import com.lagab.eventz.app.domain.order.dto.OrderResponse;
import com.lagab.eventz.app.domain.order.service.OrderService;
import com.lagab.eventz.app.domain.ticket.dto.TicketDTO;
import com.lagab.eventz.app.domain.ticket.service.TicketService;
import com.lagab.eventz.app.domain.user.repository.UserRepository;
import com.lagab.eventz.app.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private SecurityUtils securityUtils;

    @Test
    @DisplayName("GET /api/orders/user/tickets - authenticated user, tickets found")
    @WithMockCustomUser( email = "user@example.com")
    void getUsersTickets_authenticatedUser_returnsTickets() throws Exception {
        String email = "user@example.com";
        TicketDTO ticket1 = TicketDTO.builder().ticketNumber("TKT-1").build();
        TicketDTO ticket2 = TicketDTO.builder().ticketNumber("TKT-2").build();
        Page<TicketDTO> ticketPage = new PageImpl<>(List.of(ticket1, ticket2), PageRequest.of(0, 20), 2);
        when(ticketService.getTicketsByAttendeeEmail(eq(email), any())).thenReturn(ticketPage);

        mockMvc.perform(get("/api/orders/user/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ticketNumber").value("TKT-1"))
                .andExpect(jsonPath("$.content[1].ticketNumber").value("TKT-2"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /api/orders/user/tickets - unauthenticated user, returns 401")
    void getUsersTickets_unauthenticatedUser_returns401() throws Exception {
        mockMvc.perform(get("/api/orders/user/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/orders/user/tickets - with pagination works")
    @WithMockCustomUser( email = "user@example.com")
    void getUsersTickets_paginationWorks() throws Exception {
        String email = "user@example.com";
        TicketDTO ticket1 = TicketDTO.builder().ticketNumber("TKT-1").build();
        Page<TicketDTO> ticketPage = new PageImpl<>(List.of(ticket1), PageRequest.of(1, 1), 2);
        when(ticketService.getTicketsByAttendeeEmail(eq(email), any())).thenReturn(ticketPage);

        mockMvc.perform(get("/api/orders/user/tickets?page=1&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ticketNumber").value("TKT-1"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /api/orders/{orderId} - retourne l'ordre demandé")
    void getOrder_returnsOrderResponse() throws Exception {
        Long orderId = 42L;
        OrderResponse response = new OrderResponse(
                orderId,
                "ORD-42",
                null,
                null,
                null,
                null,
                null,
                List.of(),
                "Event Test",
                "event-url",
                null,
                null,
                "Paris",
                "1 rue de Paris",
                null,
                "note test",
                List.of(TicketDTO.builder().ticketNumber("TKT-42").build())
        );
        when(orderService.getOrderResponseById(orderId)).thenReturn(response);

        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-42"));
    }
}

