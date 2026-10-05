package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.exception.HotelReviewNotFoundException;
import com.agencyvoyage.application.exception.NotReviewAuthorException;
import com.agencyvoyage.application.port.in.DeleteHotelReviewCommand;
import com.agencyvoyage.application.port.out.HotelReviewRepository;
import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.hotel.HotelReview;
import com.agencyvoyage.domain.hotel.HotelReviewId;
import com.agencyvoyage.domain.user.User;
import com.agencyvoyage.domain.user.UserId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteHotelReviewServiceTest {

    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");

    @Mock
    private HotelReviewRepository hotelReviewRepository;

    private DeleteHotelReviewService service;

    @BeforeEach
    void setUp() {
        service = new DeleteHotelReviewService(hotelReviewRepository);
    }

    @Test
    void theAuthorCanDeleteTheirOwnReview() {
        User alice = new User(UserId.newId(), "alice@example.com", "Alice");
        HotelReview existing =
                new HotelReview(HotelReviewId.newId(), HotelId.newId(), alice.id(), "Alice", 3, null, NOW);
        when(hotelReviewRepository.findById(existing.id())).thenReturn(Optional.of(existing));

        service.deleteReview(new DeleteHotelReviewCommand(existing.id(), alice));

        verify(hotelReviewRepository).deleteById(existing.id());
    }

    @Test
    void anAdminCanDeleteAnyonesReview() {
        User alice = new User(UserId.newId(), "alice@example.com", "Alice");
        User admin = new User(UserId.newId(), "admin@example.com", "Admin", true);
        HotelReview existing =
                new HotelReview(HotelReviewId.newId(), HotelId.newId(), alice.id(), "Alice", 3, null, NOW);
        when(hotelReviewRepository.findById(existing.id())).thenReturn(Optional.of(existing));

        service.deleteReview(new DeleteHotelReviewCommand(existing.id(), admin));

        verify(hotelReviewRepository).deleteById(existing.id());
    }

    @Test
    void rejectsADeleteFromSomeoneWhoIsNeitherTheAuthorNorAnAdmin() {
        User alice = new User(UserId.newId(), "alice@example.com", "Alice");
        User bob = new User(UserId.newId(), "bob@example.com", "Bob");
        HotelReview existing =
                new HotelReview(HotelReviewId.newId(), HotelId.newId(), alice.id(), "Alice", 3, null, NOW);
        when(hotelReviewRepository.findById(existing.id())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.deleteReview(new DeleteHotelReviewCommand(existing.id(), bob)))
                .isInstanceOf(NotReviewAuthorException.class);
        verify(hotelReviewRepository, never()).deleteById(existing.id());
    }

    @Test
    void throwsWhenTheReviewDoesNotExist() {
        HotelReviewId unknownId = HotelReviewId.newId();
        User alice = new User(UserId.newId(), "alice@example.com", "Alice");
        when(hotelReviewRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteReview(new DeleteHotelReviewCommand(unknownId, alice)))
                .isInstanceOf(HotelReviewNotFoundException.class);
    }
}
