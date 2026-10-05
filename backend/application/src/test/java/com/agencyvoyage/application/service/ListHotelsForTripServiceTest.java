package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.port.out.HotelRepository;
import com.agencyvoyage.domain.hotel.Hotel;
import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.trip.TripId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListHotelsForTripServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    private ListHotelsForTripService service;

    @BeforeEach
    void setUp() {
        service = new ListHotelsForTripService(hotelRepository);
    }

    @Test
    void returnsWhateverTheRepositoryHasForThatTrip() {
        TripId tripId = TripId.newId();
        Hotel hotel = new Hotel(HotelId.newId(), tripId, "Ubud Retreat", "Jungle views", List.of(), List.of());
        when(hotelRepository.findByTripId(tripId)).thenReturn(List.of(hotel));

        List<Hotel> result = service.listHotels(tripId);

        assertThat(result).containsExactly(hotel);
    }
}
