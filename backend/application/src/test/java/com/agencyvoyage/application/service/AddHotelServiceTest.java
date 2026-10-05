package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.agencyvoyage.application.exception.NotAnAdminException;
import com.agencyvoyage.application.port.in.AddHotelCommand;
import com.agencyvoyage.application.port.out.HotelRepository;
import com.agencyvoyage.domain.hotel.Hotel;
import com.agencyvoyage.domain.trip.TripId;
import com.agencyvoyage.domain.user.User;
import com.agencyvoyage.domain.user.UserId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddHotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    private AddHotelService service;

    @BeforeEach
    void setUp() {
        service = new AddHotelService(hotelRepository);
    }

    @Test
    void anAdminCanAddAHotel() {
        User admin = new User(UserId.newId(), "admin@example.com", "Admin", true);
        TripId tripId = TripId.newId();

        Hotel result = service.addHotel(new AddHotelCommand(
                tripId,
                "Ubud Retreat",
                "Jungle views",
                List.of("https://example.com/photo.jpg"),
                List.of("Restaurant", "Pool"),
                admin));

        assertThat(result.tripId()).isEqualTo(tripId);
        assertThat(result.name()).isEqualTo("Ubud Retreat");
        assertThat(result.description()).isEqualTo("Jungle views");
        assertThat(result.photoUrls()).containsExactly("https://example.com/photo.jpg");
        assertThat(result.amenities()).containsExactly("Restaurant", "Pool");
        ArgumentCaptor<Hotel> captor = ArgumentCaptor.forClass(Hotel.class);
        verify(hotelRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(result);
    }

    @Test
    void rejectsANonAdminCaller() {
        User regularUser = new User(UserId.newId(), "alice@example.com", "Alice");
        TripId tripId = TripId.newId();

        assertThatThrownBy(() -> service.addHotel(
                        new AddHotelCommand(tripId, "Ubud Retreat", "Jungle views", List.of(), List.of(), regularUser)))
                .isInstanceOf(NotAnAdminException.class);
        verify(hotelRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
