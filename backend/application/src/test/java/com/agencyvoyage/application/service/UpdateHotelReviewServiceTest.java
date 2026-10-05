package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.exception.HotelReviewNotFoundException;
import com.agencyvoyage.application.exception.NotReviewAuthorException;
import com.agencyvoyage.application.port.in.UpdateHotelReviewCommand;
import com.agencyvoyage.application.port.out.HotelReviewRepository;
import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.hotel.HotelReview;
import com.agencyvoyage.domain.hotel.HotelReviewId;
import com.agencyvoyage.domain.user.User;
import com.agencyvoyage.domain.user.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateHotelReviewServiceTest {

    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");
    private static final Instant LATER = Instant.parse("2027-02-01T00:00:00Z");

    @Mock
    private HotelReviewRepository hotelReviewRepository;

    private UpdateHotelReviewService service;

    @BeforeEach
    void setUp() {
        service = new UpdateHotelReviewService(hotelReviewRepository, Clock.fixed(LATER, ZoneOffset.UTC));
    }

    @Test
    void theAuthorCanEditTheirOwnReview() {
        User alice = new User(UserId.newId(), "alice@example.com", "Alice");
        HotelReview existing = new HotelReview(
                HotelReviewId.newId(), HotelId.newId(), alice.id(), "Alice", 3, "It was ok", NOW);
        when(hotelReviewRepository.findById(existing.id())).thenReturn(Optional.of(existing));

        HotelReview result = service.updateReview(
                new UpdateHotelReviewCommand(existing.id(), 5, "Actually loved it!", alice));

        assertThat(result.rating()).isEqualTo(5);
        assertThat(result.comment()).isEqualTo("Actually loved it!");
        assertThat(result.reviewedAt()).isEqualTo(LATER);
        ArgumentCaptor<HotelReview> captor = ArgumentCaptor.forClass(HotelReview.class);
        verify(hotelReviewRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(result);
    }

    @Test
    void rejectsAnEditFromSomeoneOtherThanTheAuthor() {
        User alice = new User(UserId.newId(), "alice@example.com", "Alice");
        User bob = new User(UserId.newId(), "bob@example.com", "Bob");
        HotelReview existing =
                new HotelReview(HotelReviewId.newId(), HotelId.newId(), alice.id(), "Alice", 3, null, NOW);
        when(hotelReviewRepository.findById(existing.id())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateReview(new UpdateHotelReviewCommand(existing.id(), 1, null, bob)))
                .isInstanceOf(NotReviewAuthorException.class);
    }

    @Test
    void throwsWhenTheReviewDoesNotExist() {
        HotelReviewId unknownId = HotelReviewId.newId();
        User alice = new User(UserId.newId(), "alice@example.com", "Alice");
        when(hotelReviewRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateReview(new UpdateHotelReviewCommand(unknownId, 3, null, alice)))
                .isInstanceOf(HotelReviewNotFoundException.class);
    }
}
