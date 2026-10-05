package com.agencyvoyage.application.port.in;

import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.user.User;
import java.util.List;
import java.util.Objects;

public record UpdateHotelCommand(
        HotelId hotelId,
        String name,
        String description,
        List<String> photoUrls,
        List<String> amenities,
        User requestedBy) {

    public UpdateHotelCommand {
        Objects.requireNonNull(hotelId, "hotelId must not be null");
        Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        photoUrls = photoUrls == null ? List.of() : List.copyOf(photoUrls);
        amenities = amenities == null ? List.of() : List.copyOf(amenities);
    }
}
