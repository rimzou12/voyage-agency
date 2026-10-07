package com.agencyvoyage.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TripRequest(
        String destination,
        String description,
        LocalDate departureDate,
        LocalDate returnDate,
        int minParticipants,
        int maxParticipants,
        Instant bookingDeadline,
        BigDecimal basePrice,
        List<PriceTierRequest> priceTiers,
        List<String> photoUrls) {}
