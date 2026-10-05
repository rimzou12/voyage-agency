package com.agencyvoyage.domain.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agencyvoyage.domain.trip.TripId;
import java.util.List;
import org.junit.jupiter.api.Test;

class HotelTest {

    @Test
    void createsAValidHotelWithPhotosAndAmenities() {
        Hotel hotel = new Hotel(
                HotelId.newId(),
                TripId.newId(),
                "Ubud Retreat",
                "Jungle views",
                List.of("https://example.com/a.jpg"),
                List.of("Restaurant", "Pool"));

        assertThat(hotel.photoUrls()).containsExactly("https://example.com/a.jpg");
        assertThat(hotel.amenities()).containsExactly("Restaurant", "Pool");
    }

    @Test
    void defaultsAmenitiesToAnEmptyListWhenNull() {
        Hotel hotel = new Hotel(
                HotelId.newId(), TripId.newId(), "Ubud Retreat", "Jungle views", List.of(), null);

        assertThat(hotel.amenities()).isEmpty();
    }

    @Test
    void rejectsABlankName() {
        assertThatThrownBy(() -> new Hotel(
                        HotelId.newId(), TripId.newId(), " ", "Jungle views", List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABlankDescription() {
        assertThatThrownBy(() -> new Hotel(
                        HotelId.newId(), TripId.newId(), "Ubud Retreat", " ", List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
