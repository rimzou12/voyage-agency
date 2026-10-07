package com.agencyvoyage.web.dto;

import com.agencyvoyage.domain.trip.Trip;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TripResponse(
        String id,
        String destination,
        String description,
        LocalDate departureDate,
        LocalDate returnDate,
        int minParticipants,
        int maxParticipants,
        Instant bookingDeadline,
        BigDecimal basePrice,
        List<PriceTierResponse> priceTiers,
        List<String> photoUrls) {

    public static TripResponse from(Trip trip) {
        List<PriceTierResponse> tiers = trip.priceTiers().stream()
                .map(tier -> new PriceTierResponse(tier.minParticipants(), tier.pricePerSeat()))
                .toList();
        return new TripResponse(
                trip.id().toString(),
                trip.destination(),
                trip.description(),
                trip.departureDate(),
                trip.returnDate(),
                trip.minParticipants(),
                trip.maxParticipants(),
                trip.bookingDeadline(),
                trip.pricingSchedule().basePrice(),
                tiers,
                trip.photoUrls());
    }
}
