package com.agencyvoyage.application.port.in;

import com.agencyvoyage.domain.trip.TripId;
import com.agencyvoyage.domain.user.User;
import java.util.List;
import java.util.Objects;

public record AddHotelCommand(
        TripId tripId,
        String name,
        String description,
        List<String> photoUrls,
        List<String> amenities,
        User requestedBy) {

    public AddHotelCommand {
        Objects.requireNonNull(tripId, "tripId must not be null");
        Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        photoUrls = photoUrls == null ? List.of() : List.copyOf(photoUrls);
        amenities = amenities == null ? List.of() : List.copyOf(amenities);
    }
}
