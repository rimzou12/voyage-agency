package com.agencyvoyage.domain.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agencyvoyage.domain.user.UserId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class HotelReviewTest {

    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");

    @Test
    void createsAValidReviewWithAComment() {
        HotelReview review = new HotelReview(
                HotelReviewId.newId(), HotelId.newId(), UserId.newId(), "Alice", 5, "Loved it!", NOW);

        assertThat(review.rating()).isEqualTo(5);
        assertThat(review.comment()).isEqualTo("Loved it!");
    }

    @Test
    void normalizesABlankCommentToNull() {
        HotelReview review =
                new HotelReview(HotelReviewId.newId(), HotelId.newId(), UserId.newId(), "Alice", 4, "  ", NOW);

        assertThat(review.comment()).isNull();
    }

    @Test
    void allowsANullComment() {
        HotelReview review =
                new HotelReview(HotelReviewId.newId(), HotelId.newId(), UserId.newId(), "Alice", 3, null, NOW);

        assertThat(review.comment()).isNull();
    }

    @Test
    void rejectsARatingBelowOne() {
        assertThatThrownBy(() -> new HotelReview(
                        HotelReviewId.newId(), HotelId.newId(), UserId.newId(), "Alice", 0, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsARatingAboveFive() {
        assertThatThrownBy(() -> new HotelReview(
                        HotelReviewId.newId(), HotelId.newId(), UserId.newId(), "Alice", 6, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABlankAuthorName() {
        assertThatThrownBy(
                        () -> new HotelReview(HotelReviewId.newId(), HotelId.newId(), UserId.newId(), " ", 3, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
