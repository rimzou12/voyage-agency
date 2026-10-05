package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.exception.AlreadyReviewedHotelException;
import com.agencyvoyage.application.port.in.AddHotelReviewCommand;
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
class AddHotelReviewServiceTest {

    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");

    @Mock
    private HotelReviewRepository hotelReviewRepository;

    private AddHotelReviewService service;

    @BeforeEach
    void setUp() {
        service = new AddHotelReviewService(hotelReviewRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void aLoggedInUserCanReviewAHotel() {
        User alice = new User(UserId.newId(), "alice@example.com", "Alice");
        HotelId hotelId = HotelId.newId();
        when(hotelReviewRepository.findByHotelIdAndAuthorId(hotelId, alice.id())).thenReturn(Optional.empty());

        HotelReview result = service.addReview(new AddHotelReviewCommand(hotelId, 5, "Loved it!", alice));

        assertThat(result.hotelId()).isEqualTo(hotelId);
        assertThat(result.authorId()).isEqualTo(alice.id());
        assertThat(result.authorName()).isEqualTo("Alice");
        assertThat(result.rating()).isEqualTo(5);
        assertThat(result.comment()).isEqualTo("Loved it!");
        assertThat(result.reviewedAt()).isEqualTo(NOW);
        ArgumentCaptor<HotelReview> captor = ArgumentCaptor.forClass(HotelReview.class);
        verify(hotelReviewRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(result);
    }

    @Test
    void rejectsASecondReviewFromTheSameUserForTheSameHotel() {
        User alice = new User(UserId.newId(), "alice@example.com", "Alice");
        HotelId hotelId = HotelId.newId();
        HotelReview existing =
                new HotelReview(HotelReviewId.newId(), hotelId, alice.id(), "Alice", 4, null, NOW);
        when(hotelReviewRepository.findByHotelIdAndAuthorId(hotelId, alice.id())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.addReview(new AddHotelReviewCommand(hotelId, 5, null, alice)))
                .isInstanceOf(AlreadyReviewedHotelException.class);
    }
}
