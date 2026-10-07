package com.agencyvoyage.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.agencyvoyage.domain.trip.PriceTier;
import com.agencyvoyage.domain.trip.PricingSchedule;
import com.agencyvoyage.domain.trip.Trip;
import com.agencyvoyage.domain.trip.TripId;
import com.agencyvoyage.infrastructure.config.AbstractPostgresIT;
import com.agencyvoyage.infrastructure.persistence.jpa.adapter.TripRepositoryAdapter;
import com.agencyvoyage.infrastructure.persistence.jpa.mapper.TripMapper;
import com.agencyvoyage.infrastructure.persistence.jpa.repository.SpringDataTripJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TripRepositoryAdapterIT extends AbstractPostgresIT {

    @Autowired
    private TripRepositoryAdapter adapter;

    @Autowired
    private SpringDataTripJpaRepository springDataRepository;

    @Test
    void roundTripsATripWithItsPriceTiersThroughRealPostgres() {
        Trip trip = trip();
        springDataRepository.save(TripMapper.toEntity(trip));

        Optional<Trip> found = adapter.findById(trip.id());

        assertThat(found).isPresent();
        Trip reloaded = found.get();
        assertThat(reloaded.destination()).isEqualTo("Bali");
        assertThat(reloaded.priceTiers()).extracting(PriceTier::minParticipants).containsExactly(2, 4);
        assertThat(reloaded.pricingSchedule().priceFor(4)).isEqualByComparingTo("600");
    }

    @Test
    void findAllReturnsEveryPersistedTrip() {
        Trip trip = trip();
        springDataRepository.save(TripMapper.toEntity(trip));

        List<Trip> all = adapter.findAll();

        assertThat(all).extracting(Trip::id).contains(trip.id());
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        assertThat(adapter.findById(TripId.newId())).isEmpty();
    }

    @Test
    void saveCreatesANewTrip() {
        Trip trip = trip();

        adapter.save(trip);

        assertThat(adapter.findById(trip.id())).contains(trip);
    }

    @Test
    void deleteByIdRemovesTheTrip() {
        Trip trip = trip();
        adapter.save(trip);

        adapter.deleteById(trip.id());

        assertThat(adapter.findById(trip.id())).isEmpty();
    }

    @Test
    void saveUpdatesAnExistingTripAndReplacesItsPriceTiers() {
        Trip trip = trip();
        adapter.save(trip);

        PricingSchedule updatedSchedule =
                PricingSchedule.of(new BigDecimal("2000"), List.of(new PriceTier(3, new BigDecimal("1500"))), 10);
        Trip updated = new Trip(
                trip.id(),
                "Kyoto",
                "7 days in Kyoto",
                trip.departureDate(),
                trip.returnDate(),
                trip.minParticipants(),
                trip.maxParticipants(),
                trip.bookingDeadline(),
                updatedSchedule,
                List.of());
        adapter.save(updated);

        Trip reloaded = adapter.findById(trip.id()).orElseThrow();
        assertThat(reloaded.destination()).isEqualTo("Kyoto");
        assertThat(reloaded.priceTiers()).extracting(PriceTier::minParticipants).containsExactly(3);
        assertThat(reloaded.pricingSchedule().priceFor(3)).isEqualByComparingTo("1500");
    }

    private static Trip trip() {
        PricingSchedule schedule = PricingSchedule.of(
                new BigDecimal("1000"),
                List.of(new PriceTier(2, new BigDecimal("800")), new PriceTier(4, new BigDecimal("600"))),
                10);
        return new Trip(
                TripId.newId(),
                "Bali",
                "10 days in Bali",
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 20),
                2,
                10,
                Instant.now().plus(30, ChronoUnit.DAYS),
                schedule,
                List.of());
    }
}
