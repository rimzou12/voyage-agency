package com.agencyvoyage.application.port.in;

import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.user.User;
import java.util.Objects;

public record AddHotelReviewCommand(HotelId hotelId, int rating, String comment, User author) {

    public AddHotelReviewCommand {
        Objects.requireNonNull(hotelId, "hotelId must not be null");
        Objects.requireNonNull(author, "author must not be null");
    }
}
