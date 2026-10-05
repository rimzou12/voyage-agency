package com.agencyvoyage.domain.hotel;

import java.util.Objects;
import java.util.UUID;

public record HotelReviewId(UUID value) {

    public HotelReviewId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static HotelReviewId newId() {
        return new HotelReviewId(UUID.randomUUID());
    }

    public static HotelReviewId of(String value) {
        return new HotelReviewId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
