package com.agencyvoyage.infrastructure.seed;

import com.agencyvoyage.domain.trip.PriceTier;
import com.agencyvoyage.domain.trip.PricingSchedule;
import com.agencyvoyage.domain.trip.Trip;
import com.agencyvoyage.domain.trip.TripId;
import com.agencyvoyage.infrastructure.persistence.jpa.mapper.TripMapper;
import com.agencyvoyage.infrastructure.persistence.jpa.repository.SpringDataTripJpaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds a handful of sample trips on startup if the catalog is empty. Trips are the
 * agency's read-only offering in this pass (no admin CRUD yet), so this is the only
 * way trip data gets into the database.
 */
@Component
public class TripCatalogSeeder implements CommandLineRunner {

    private final SpringDataTripJpaRepository tripRepository;
    private final Clock clock;

    public TripCatalogSeeder(SpringDataTripJpaRepository tripRepository, Clock clock) {
        this.tripRepository = Objects.requireNonNull(tripRepository, "tripRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void run(String... args) {
        if (tripRepository.count() > 0) {
            return;
        }
        sampleTrips(clock.instant()).forEach(trip -> tripRepository.save(TripMapper.toEntity(trip)));
    }

    private List<Trip> sampleTrips(Instant now) {
        return List.of(
                new Trip(
                        TripId.newId(),
                        "Bali, Indonesia",
                        "10 days across Ubud, Seminyak and the Gili Islands.",
                        LocalDate.now().plusMonths(3),
                        LocalDate.now().plusMonths(3).plusDays(10),
                        4,
                        12,
                        now.plus(21, ChronoUnit.DAYS),
                        PricingSchedule.of(
                                new BigDecimal("1450.00"),
                                List.of(
                                        new PriceTier(6, new BigDecimal("1250.00")),
                                        new PriceTier(9, new BigDecimal("1090.00"))),
                                12),
                        List.of()),
                new Trip(
                        TripId.newId(),
                        "Kyoto, Japan",
                        "7 days of temples, gardens and a day trip to Nara.",
                        LocalDate.now().plusMonths(4),
                        LocalDate.now().plusMonths(4).plusDays(7),
                        3,
                        8,
                        now.plus(14, ChronoUnit.DAYS),
                        PricingSchedule.of(
                                new BigDecimal("1980.00"),
                                List.of(new PriceTier(5, new BigDecimal("1690.00"))),
                                8),
                        List.of()),
                new Trip(
                        TripId.newId(),
                        "Marrakech, Morocco",
                        "5 days in the medina with a Sahara desert overnight.",
                        LocalDate.now().plusMonths(2),
                        LocalDate.now().plusMonths(2).plusDays(5),
                        6,
                        20,
                        now.plus(10, ChronoUnit.DAYS),
                        PricingSchedule.of(
                                new BigDecimal("890.00"),
                                List.of(
                                        new PriceTier(10, new BigDecimal("740.00")),
                                        new PriceTier(15, new BigDecimal("650.00"))),
                                20),
                        List.of()));
    }
}
