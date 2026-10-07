package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.exception.NotAnAdminException;
import com.agencyvoyage.application.exception.TripNotFoundException;
import com.agencyvoyage.application.port.in.DeleteTripCommand;
import com.agencyvoyage.application.port.out.TripRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteTripServiceTest {

    private static final User ADMIN = new User(UserId.newId(), "admin@example.com", "Admin", true);
    private static final User REGULAR_USER = new User(UserId.newId(), "alice@example.com", "Alice");

    @Mock
    private TripRepository tripRepository;

    private DeleteTripService service;

    @BeforeEach
    void setUp() {
        service = new DeleteTripService(tripRepository);
    }

    @Test
    void anAdminCanDeleteAnExistingTrip() {
        Trip existing = existingTrip();
        when(tripRepository.findById(existing.id())).thenReturn(Optional.of(existing));

        service.deleteTrip(new DeleteTripCommand(existing.id(), ADMIN));

        verify(tripRepository).deleteById(existing.id());
    }

    @Test
    void rejectsANonAdminCaller() {
        Trip existing = existingTrip();

        assertThatThrownBy(() -> service.deleteTrip(new DeleteTripCommand(existing.id(), REGULAR_USER)))
                .isInstanceOf(NotAnAdminException.class);
        verify(tripRepository, never()).deleteById(existing.id());
    }

    @Test
    void throwsWhenTheTripDoesNotExist() {
        TripId unknownId = TripId.newId();
        when(tripRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteTrip(new DeleteTripCommand(unknownId, ADMIN)))
                .isInstanceOf(TripNotFoundException.class);
        verify(tripRepository, never()).deleteById(unknownId);
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
}
