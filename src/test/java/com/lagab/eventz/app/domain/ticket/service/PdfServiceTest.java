package com.lagab.eventz.app.domain.ticket.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.lagab.eventz.app.domain.ticket.dto.TicketDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private PdfService pdfService;

    @Test
    void generateTicket_shouldUseFrenchLocaleAndSetContextVariables_andReturnNonEmptyPdf() {
        // Arrange

        // Minimal valid XHTML for Flying Saucer
        String xhtml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
                        "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
                        "  <head>" +
                        "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />" +
                        "    <title>Test Ticket</title>" +
                        "  </head>" +
                        "  <body>" +
                        "    <p>Bonjour Ticket</p>" +
                        "  </body>" +
                        "</html>";

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        when(templateEngine.process(eq("ticket/ticket-template"), contextCaptor.capture())).thenReturn(xhtml);

        TicketDTO ticket = TicketDTO.builder()
                                    .eventName("Événement Test")
                                    .surtitle("Sur-titre")
                                    .subtitle("Sous-titre")
                                    .startDate(LocalDateTime.now())
                                    .endDate(LocalDateTime.now().plusHours(2))
                                    .venueName("Salle des Fêtes")
                                    .venueAddress("1 Rue de la Paix")
                                    .venueCity("Paris")
                                    .venueCountry("France")
                                    .buyerName("Jean Dupont")
                                    .ticketType("Standard")
                                    .ticketNumber("TCK-123456")
                                    .qrCode("fake-qrcode-bytes")
                                    .organizerName("Org")
                                    .organizerEmail("org@example.com")
                                    .organizerWebsite("https://example.com")
                                    .orderNumber("ORD-7890")
                                    .orderDate(LocalDateTime.now())
                                    .ticketId("42")
                                    .price(10.0)
                                    .build();

        // Act
        byte[] pdfBytes = pdfService.generateTicket(ticket);

        // Assert: template processing interaction
        verify(templateEngine, times(1)).process(eq("ticket/ticket-template"), any(Context.class));

        Context usedContext = contextCaptor.getValue();
        assertNotNull(usedContext, "Thymeleaf Context should be provided");
        assertEquals(Locale.FRENCH, usedContext.getLocale(), "Locale should be French");

        Object ticketVar = usedContext.getVariable("ticket");
        assertSame(ticket, ticketVar, "Context should contain the provided ticket under 'ticket'");

        Object dateFormatter = usedContext.getVariable("dateFormatter");
        Object timeFormatter = usedContext.getVariable("timeFormatter");
        assertNotNull(dateFormatter, "Context should contain 'dateFormatter'");
        assertNotNull(timeFormatter, "Context should contain 'timeFormatter'");
        assertInstanceOf(DateTimeFormatter.class, dateFormatter, "dateFormatter must be a DateTimeFormatter");
        assertInstanceOf(DateTimeFormatter.class, timeFormatter, "timeFormatter must be a DateTimeFormatter");

        // Assert: PDF result looks valid (non-empty)
        assertNotNull(pdfBytes, "PDF bytes should not be null");
        assertTrue(pdfBytes.length > 0, "PDF bytes should not be empty");
    }
}
