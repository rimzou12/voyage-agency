package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.port.out.HotelReviewRepository;
import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.hotel.HotelReview;
import com.agencyvoyage.domain.hotel.HotelReviewId;
import com.agencyvoyage.domain.user.UserId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListHotelReviewsServiceTest {

    @Mock
    private HotelReviewRepository hotelReviewRepository;

    @Test
    void returnsWhateverTheRepositoryHasForThatHotel() {
        ListHotelReviewsService service = new ListHotelReviewsService(hotelReviewRepository);
        HotelId hotelId = HotelId.newId();
        HotelReview review = new HotelReview(
                HotelReviewId.newId(),
                hotelId,
                UserId.newId(),
                "Alice",
                5,
                "Great stay",
                Instant.parse("2027-01-01T00:00:00Z"));
        when(hotelReviewRepository.findByHotelId(hotelId)).thenReturn(List.of(review));

        assertThat(service.listReviews(hotelId)).containsExactly(review);
    }
}
