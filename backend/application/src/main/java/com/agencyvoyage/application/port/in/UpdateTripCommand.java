package com.agencyvoyage.application.port.in;

import com.agencyvoyage.domain.trip.PriceTier;
import com.agencyvoyage.domain.trip.TripId;
import com.agencyvoyage.domain.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record UpdateTripCommand(
        TripId tripId,
        String destination,
        String description,
        LocalDate departureDate,
        LocalDate returnDate,
        int minParticipants,
        int maxParticipants,
        Instant bookingDeadline,
        BigDecimal basePrice,
        List<PriceTier> priceTiers,
        List<String> photoUrls,
        User requestedBy) {

    public UpdateTripCommand {
        Objects.requireNonNull(tripId, "tripId must not be null");
        Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        priceTiers = priceTiers == null ? List.of() : List.copyOf(priceTiers);
        photoUrls = photoUrls == null ? List.of() : List.copyOf(photoUrls);
    }
}
