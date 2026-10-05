package com.agencyvoyage.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.hotel.HotelReview;
import com.agencyvoyage.domain.hotel.HotelReviewId;
import com.agencyvoyage.domain.user.UserId;
import com.agencyvoyage.infrastructure.config.AbstractPostgresIT;
import com.agencyvoyage.infrastructure.persistence.jpa.adapter.HotelReviewRepositoryAdapter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class HotelReviewRepositoryAdapterIT extends AbstractPostgresIT {

    @Autowired
    private HotelReviewRepositoryAdapter adapter;

    @Test
    void savesAndReloadsAReviewWithAComment() {
        // Postgres' timestamp column stores microsecond precision; Instant.now() carries
        // nanoseconds, so round-tripping an untruncated instant would never compare equal.
        HotelReview review = new HotelReview(
                HotelReviewId.newId(),
                HotelId.newId(),
                UserId.newId(),
                "Alice",
                5,
                "Loved it!",
                Instant.now().truncatedTo(ChronoUnit.MICROS));

        adapter.save(review);

        Optional<HotelReview> reloaded = adapter.findById(review.id());
        assertThat(reloaded).contains(review);
    }

    @Test
    void savesAndReloadsAReviewWithNoComment() {
        HotelReview review = new HotelReview(
                HotelReviewId.newId(),
                HotelId.newId(),
                UserId.newId(),
                "Alice",
                3,
                null,
                Instant.now().truncatedTo(ChronoUnit.MICROS));

        adapter.save(review);

        HotelReview reloaded = adapter.findById(review.id()).orElseThrow();
        assertThat(reloaded.comment()).isNull();
    }

    @Test
    void returnsReviewsForAHotelNewestFirst() {
        HotelId hotelId = HotelId.newId();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        HotelReview older =
                new HotelReview(HotelReviewId.newId(), hotelId, UserId.newId(), "Alice", 4, "Good", now);
        HotelReview newer = new HotelReview(
                HotelReviewId.newId(), hotelId, UserId.newId(), "Bob", 5, "Great", now.plus(1, ChronoUnit.MINUTES));
        adapter.save(older);
        adapter.save(newer);
        adapter.save(new HotelReview(
                HotelReviewId.newId(), HotelId.newId(), UserId.newId(), "Carol", 2, "Meh", now));

        List<HotelReview> found = adapter.findByHotelId(hotelId);

        assertThat(found).containsExactly(newer, older);
    }

    @Test
    void findsAUsersExistingReviewForAHotel() {
        HotelId hotelId = HotelId.newId();
        UserId authorId = UserId.newId();
        HotelReview review = new HotelReview(
                HotelReviewId.newId(),
                hotelId,
                authorId,
                "Alice",
                5,
                null,
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        adapter.save(review);

        assertThat(adapter.findByHotelIdAndAuthorId(hotelId, authorId)).contains(review);
        assertThat(adapter.findByHotelIdAndAuthorId(hotelId, UserId.newId())).isEmpty();
    }

    @Test
    void deleteRemovesTheReview() {
        HotelReview review = new HotelReview(
                HotelReviewId.newId(),
                HotelId.newId(),
                UserId.newId(),
                "Alice",
                5,
                null,
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        adapter.save(review);

        adapter.deleteById(review.id());

        assertThat(adapter.findById(review.id())).isEmpty();
    }
}
