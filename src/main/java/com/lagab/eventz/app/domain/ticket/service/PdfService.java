package com.lagab.eventz.app.domain.ticket.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lagab.eventz.app.domain.ticket.dto.TicketDTO;
import com.lowagie.text.DocumentException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final TemplateEngine templateEngine;

    public byte[] generateTicket(TicketDTO ticket) {

        // Create Thymeleaf context
        Context context = new Context(Locale.FRENCH);
        context.setVariable("ticket", ticket);
        context.setVariable("dateFormatter", DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH));
        context.setVariable("timeFormatter", DateTimeFormatter.ofPattern("HH:mm"));

        // Generate HTML
        String htmlContent = templateEngine.process("ticket/ticket-template", context);

        // Convert to pdf
        return convertHtmlToPdf(htmlContent);
    }

    private byte[] convertHtmlToPdf(String htmlContent) throws DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(htmlContent);
        renderer.layout();
        renderer.createPDF(outputStream);

        return outputStream.toByteArray();
    }

}
