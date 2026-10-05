package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.exception.HotelNotFoundException;
import com.agencyvoyage.application.exception.NotAnAdminException;
import com.agencyvoyage.application.port.in.DeleteHotelCommand;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteHotelServiceTest {

    private static final User ADMIN = new User(UserId.newId(), "admin@example.com", "Admin", true);
    private static final User REGULAR_USER = new User(UserId.newId(), "alice@example.com", "Alice");

    @Mock
    private HotelRepository hotelRepository;

    private DeleteHotelService service;

    @BeforeEach
    void setUp() {
        service = new DeleteHotelService(hotelRepository);
    }

    @Test
    void anAdminCanDeleteAnExistingHotel() {
        Hotel existing = existingHotel();
        when(hotelRepository.findById(existing.id())).thenReturn(Optional.of(existing));

        service.deleteHotel(new DeleteHotelCommand(existing.id(), ADMIN));

        verify(hotelRepository).deleteById(existing.id());
    }

    @Test
    void rejectsANonAdminCaller() {
        Hotel existing = existingHotel();

        assertThatThrownBy(() -> service.deleteHotel(new DeleteHotelCommand(existing.id(), REGULAR_USER)))
                .isInstanceOf(NotAnAdminException.class);
        verify(hotelRepository, never()).deleteById(existing.id());
    }

    @Test
    void throwsWhenTheHotelDoesNotExist() {
        HotelId unknownId = HotelId.newId();
        when(hotelRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteHotel(new DeleteHotelCommand(unknownId, ADMIN)))
                .isInstanceOf(HotelNotFoundException.class);
        verify(hotelRepository, never()).deleteById(unknownId);
    }

    private static Hotel existingHotel() {
        return new Hotel(HotelId.newId(), TripId.newId(), "Ubud Retreat", "Jungle views", List.of(), List.of());
    }
}
