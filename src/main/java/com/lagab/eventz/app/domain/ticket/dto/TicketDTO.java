package com.lagab.eventz.app.domain.ticket.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketDTO {
    private String eventName;
    private String eventUrl;
    private String surtitle;
    private String subtitle;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String venueName;
    private String venueAddress;
    private String venueCity;
    private String venueCountry;
    private String buyerName;
    private String buyerEmail;
    private String ticketType;
    private String ticketNumber;
    private String qrCode;
    private String barcodeNumber;
    private String organizerName;
    private String organizerPhone;
    private String organizerEmail;
    private String organizerWebsite;
    private String orderNumber;
    private LocalDateTime orderDate;
    private String ticketId;
    private double price;
}
