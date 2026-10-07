package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.exception.NotAnAdminException;
import com.agencyvoyage.application.exception.TripNotFoundException;
import com.agencyvoyage.application.port.in.UpdateTripCommand;
import com.agencyvoyage.application.port.out.TripRepository;
import com.agencyvoyage.domain.trip.PriceTier;
import com.agencyvoyage.domain.trip.PricingSchedule;
import com.agencyvoyage.domain.trip.Trip;
import com.agencyvoyage.domain.trip.TripId;
import com.agencyvoyage.domain.user.User;
import com.agencyvoyage.domain.user.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateTripServiceTest {

    private static final User ADMIN = new User(UserId.newId(), "admin@example.com", "Admin", true);
    private static final User REGULAR_USER = new User(UserId.newId(), "alice@example.com", "Alice");

    @Mock
    private TripRepository tripRepository;

    private UpdateTripService service;

    @BeforeEach
    void setUp() {
        service = new UpdateTripService(tripRepository);
    }

    @Test
    void anAdminCanUpdateAnExistingTrip() {
        Trip existing = existingTrip();
        when(tripRepository.findById(existing.id())).thenReturn(Optional.of(existing));

        Trip result = service.updateTrip(command(existing.id(), ADMIN));

        assertThat(result.id()).isEqualTo(existing.id());
        assertThat(result.destination()).isEqualTo("Kyoto, Japan");
        ArgumentCaptor<Trip> captor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(result);
    }

    @Test
    void rejectsANonAdminCaller() {
        Trip existing = existingTrip();

        assertThatThrownBy(() -> service.updateTrip(command(existing.id(), REGULAR_USER)))
                .isInstanceOf(NotAnAdminException.class);
        verify(tripRepository, never()).save(any());
    }

    @Test
    void throwsWhenTheTripDoesNotExist() {
        TripId unknownId = TripId.newId();
        when(tripRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTrip(command(unknownId, ADMIN)))
                .isInstanceOf(TripNotFoundException.class);
    }

    private static Trip existingTrip() {
        PricingSchedule schedule = PricingSchedule.of(new BigDecimal("1000"), List.of(), 10);
        return new Trip(
                TripId.newId(),
                "Bali, Indonesia",
                "desc",
                LocalDate.now().plusMonths(3),
                LocalDate.now().plusMonths(3).plusDays(10),
                2,
                10,
                Instant.now().plus(21, ChronoUnit.DAYS),
                schedule,
                List.of());
    }

    private static UpdateTripCommand command(TripId tripId, User requestedBy) {
        Instant now = Instant.now();
        return new UpdateTripCommand(
                tripId,
                "Kyoto, Japan",
                "7 days of temples and gardens",
                LocalDate.now().plusMonths(4),
                LocalDate.now().plusMonths(4).plusDays(7),
                3,
                8,
                now.plus(14, ChronoUnit.DAYS),
                new BigDecimal("1980"),
                List.of(new PriceTier(5, new BigDecimal("1690"))),
                List.of(),
                requestedBy);
    }
}
