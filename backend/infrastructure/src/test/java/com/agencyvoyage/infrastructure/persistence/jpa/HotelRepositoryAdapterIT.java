package com.agencyvoyage.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.agencyvoyage.domain.hotel.Hotel;
import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.trip.TripId;
import com.agencyvoyage.infrastructure.config.AbstractPostgresIT;
import com.agencyvoyage.infrastructure.persistence.jpa.adapter.HotelRepositoryAdapter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class HotelRepositoryAdapterIT extends AbstractPostgresIT {

    @Autowired
    private HotelRepositoryAdapter adapter;

    @Test
    void savesAndReloadsAHotelWithItsPhotosAndAmenities() {
        TripId tripId = TripId.newId();
        Hotel hotel = new Hotel(
                HotelId.newId(),
                tripId,
                "Ubud Retreat",
                "Jungle views",
                List.of("https://example.com/a.jpg", "https://example.com/b.jpg"),
                List.of("Restaurant", "Pool", "Free Wi-Fi"));

        adapter.save(hotel);

        Optional<Hotel> reloaded = adapter.findById(hotel.id());
        assertThat(reloaded).contains(hotel);
    }

    @Test
    void findsOnlyHotelsForTheGivenTrip() {
        TripId tripId = TripId.newId();
        Hotel hotel = new Hotel(HotelId.newId(), tripId, "Ubud Retreat", "Jungle views", List.of(), List.of());
        adapter.save(hotel);
        adapter.save(
                new Hotel(HotelId.newId(), TripId.newId(), "Other Trip Hotel", "Elsewhere", List.of(), List.of()));

        List<Hotel> found = adapter.findByTripId(tripId);

        assertThat(found).containsExactly(hotel);
    }

    @Test
    void saveUpdatesAnExistingHotelAndReplacesItsPhotosAndAmenities() {
        TripId tripId = TripId.newId();
        Hotel hotel = new Hotel(
                HotelId.newId(), tripId, "Ubud Retreat", "Jungle views", List.of("https://x/a.jpg"), List.of("Pool"));
        adapter.save(hotel);

        Hotel updated = new Hotel(
                hotel.id(),
                tripId,
                "Renamed Retreat",
                "Updated views",
                List.of("https://x/new.jpg"),
                List.of("Restaurant", "Spa"));
        adapter.save(updated);

        Hotel reloaded = adapter.findById(hotel.id()).orElseThrow();
        assertThat(reloaded.name()).isEqualTo("Renamed Retreat");
        assertThat(reloaded.description()).isEqualTo("Updated views");
        assertThat(reloaded.photoUrls()).containsExactly("https://x/new.jpg");
        assertThat(reloaded.amenities()).containsExactly("Restaurant", "Spa");
    }

    @Test
    void deleteRemovesTheHotel() {
        Hotel hotel = new Hotel(HotelId.newId(), TripId.newId(), "Ubud Retreat", "Jungle views", List.of(), List.of());
        adapter.save(hotel);

        adapter.deleteById(hotel.id());

        assertThat(adapter.findById(hotel.id())).isEmpty();
    }
}
