package com.agencyvoyage.domain.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class TripTest {

    private static final PricingSchedule SCHEDULE = PricingSchedule.of(new BigDecimal("1000"), List.of(), 10);

    @Test
    void acceptsNewGroupBookingsBeforeTheDeadline() {
        Instant deadline = Instant.now().plus(1, ChronoUnit.DAYS);
        Trip trip = trip(deadline);

        assertThat(trip.acceptsNewGroupBookingsAt(deadline.minusSeconds(1))).isTrue();
        assertThat(trip.acceptsNewGroupBookingsAt(deadline)).isFalse();
        assertThat(trip.acceptsNewGroupBookingsAt(deadline.plusSeconds(1))).isFalse();
    }

    @Test
    void rejectsDepartureOnOrAfterReturn() {
        assertThatThrownBy(() -> new Trip(
                        TripId.newId(),
                        "Bali",
                        "desc",
                        LocalDate.of(2027, 6, 10),
                        LocalDate.of(2027, 6, 10),
                        2,
                        10,
                        Instant.now().plus(1, ChronoUnit.DAYS),
                        SCHEDULE,
                        List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("departureDate");
    }

    @Test
    void rejectsMaxParticipantsBelowMin() {
        assertThatThrownBy(() -> new Trip(
                        TripId.newId(),
                        "Bali",
                        "desc",
                        LocalDate.of(2027, 6, 10),
                        LocalDate.of(2027, 6, 20),
                        10,
                        5,
                        Instant.now().plus(1, ChronoUnit.DAYS),
                        SCHEDULE,
                        List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxParticipants");
    }

    @Test
    void rejectsBlankDestination() {
        assertThatThrownBy(() -> new Trip(
                        TripId.newId(),
                        "  ",
                        "desc",
                        LocalDate.of(2027, 6, 10),
                        LocalDate.of(2027, 6, 20),
                        2,
                        10,
                        Instant.now().plus(1, ChronoUnit.DAYS),
                        SCHEDULE,
                        List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("destination");
    }

    private static Trip trip(Instant deadline) {
        return new Trip(
                TripId.newId(),
                "Bali",
                "10 days in Bali",
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 20),
                2,
                10,
                deadline,
                SCHEDULE,
                List.of());
    }
}
