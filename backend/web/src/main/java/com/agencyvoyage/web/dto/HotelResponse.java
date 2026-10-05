package com.agencyvoyage.web.dto;

import com.agencyvoyage.domain.hotel.Hotel;
import java.util.List;

public record HotelResponse(
        String id, String tripId, String name, String description, List<String> photoUrls, List<String> amenities) {

    public static HotelResponse from(Hotel hotel) {
        return new HotelResponse(
                hotel.id().toString(),
                hotel.tripId().toString(),
                hotel.name(),
                hotel.description(),
                hotel.photoUrls(),
                hotel.amenities());
    }
}
