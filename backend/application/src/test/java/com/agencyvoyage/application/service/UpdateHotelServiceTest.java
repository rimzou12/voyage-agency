package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.exception.HotelNotFoundException;
import com.agencyvoyage.application.exception.NotAnAdminException;
import com.agencyvoyage.application.port.in.UpdateHotelCommand;
import com.agencyvoyage.application.port.out.HotelRepository;
import com.agencyvoyage.domain.hotel.Hotel;
import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.trip.TripId;
import com.agencyvoyage.domain.user.User;
import com.agencyvoyage.domain.user.UserId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateHotelServiceTest {

    private static final User ADMIN = new User(UserId.newId(), "admin@example.com", "Admin", true);
    private static final User REGULAR_USER = new User(UserId.newId(), "alice@example.com", "Alice");

    @Mock
    private HotelRepository hotelRepository;

    private UpdateHotelService service;

    @BeforeEach
    void setUp() {
        service = new UpdateHotelService(hotelRepository);
    }

    @Test
    void anAdminCanUpdateAnExistingHotel() {
        Hotel existing = existingHotel();
        when(hotelRepository.findById(existing.id())).thenReturn(Optional.of(existing));

        Hotel result = service.updateHotel(new UpdateHotelCommand(
                existing.id(),
                "Renamed Retreat",
                "Updated views",
                List.of("https://x/new.jpg"),
                List.of("Spa"),
                ADMIN));

        assertThat(result.id()).isEqualTo(existing.id());
        assertThat(result.tripId()).isEqualTo(existing.tripId());
        assertThat(result.name()).isEqualTo("Renamed Retreat");
        assertThat(result.description()).isEqualTo("Updated views");
        assertThat(result.photoUrls()).containsExactly("https://x/new.jpg");
        assertThat(result.amenities()).containsExactly("Spa");
        ArgumentCaptor<Hotel> captor = ArgumentCaptor.forClass(Hotel.class);
        verify(hotelRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(result);
    }

    @Test
    void rejectsANonAdminCaller() {
        Hotel existing = existingHotel();

        assertThatThrownBy(() -> service.updateHotel(new UpdateHotelCommand(
                        existing.id(), "Renamed", "Updated", List.of(), List.of(), REGULAR_USER)))
                .isInstanceOf(NotAnAdminException.class);
        verify(hotelRepository, never()).save(any());
    }

    @Test
    void throwsWhenTheHotelDoesNotExist() {
        HotelId unknownId = HotelId.newId();
        when(hotelRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateHotel(
                        new UpdateHotelCommand(unknownId, "Renamed", "Updated", List.of(), List.of(), ADMIN)))
                .isInstanceOf(HotelNotFoundException.class);
    }

    private static Hotel existingHotel() {
        return new Hotel(HotelId.newId(), TripId.newId(), "Ubud Retreat", "Jungle views", List.of(), List.of());
    }
}
