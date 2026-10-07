package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.exception.TripNotFoundException;
import com.agencyvoyage.application.port.out.TripRepository;
import com.agencyvoyage.domain.trip.PricingSchedule;
import com.agencyvoyage.domain.trip.Trip;
import com.agencyvoyage.domain.trip.TripId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripQueryServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Test
    void listsAllTrips() {
        TripQueryService service = new TripQueryService(tripRepository);
        Trip trip = trip();
        when(tripRepository.findAll()).thenReturn(List.of(trip));

        assertThat(service.listTrips()).containsExactly(trip);
    }

    @Test
    void getsATripById() {
        TripQueryService service = new TripQueryService(tripRepository);
        Trip trip = trip();
        when(tripRepository.findById(trip.id())).thenReturn(Optional.of(trip));

        assertThat(service.getTrip(trip.id())).isEqualTo(trip);
    }

    @Test
    void throwsWhenTheTripDoesNotExist() {
        TripQueryService service = new TripQueryService(tripRepository);
        TripId unknownId = TripId.newId();
        when(tripRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTrip(unknownId)).isInstanceOf(TripNotFoundException.class);
    }

    private static Trip trip() {
        PricingSchedule schedule = PricingSchedule.of(new BigDecimal("1000"), List.of(), 5);
        return new Trip(
                TripId.newId(),
                "Bali",
                "10 days in Bali",
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 20),
                2,
                5,
                Instant.now().plus(1, ChronoUnit.DAYS),
                schedule,
                List.of());
    }
}
