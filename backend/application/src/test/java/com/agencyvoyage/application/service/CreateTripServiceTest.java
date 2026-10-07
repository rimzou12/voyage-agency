package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.agencyvoyage.application.exception.NotAnAdminException;
import com.agencyvoyage.application.port.in.CreateTripCommand;
import com.agencyvoyage.application.port.out.TripRepository;
import com.agencyvoyage.domain.trip.PriceTier;
import com.agencyvoyage.domain.trip.Trip;
import com.agencyvoyage.domain.user.User;
import com.agencyvoyage.domain.user.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateTripServiceTest {

    private static final User ADMIN = new User(UserId.newId(), "admin@example.com", "Admin", true);
    private static final User REGULAR_USER = new User(UserId.newId(), "alice@example.com", "Alice");

    @Mock
    private TripRepository tripRepository;

    private CreateTripService service;

    @BeforeEach
    void setUp() {
        service = new CreateTripService(tripRepository);
    }

    @Test
    void anAdminCanCreateATrip() {
        Trip result = service.createTrip(command(ADMIN));

        assertThat(result.destination()).isEqualTo("Bali, Indonesia");
        assertThat(result.minParticipants()).isEqualTo(2);
        assertThat(result.maxParticipants()).isEqualTo(10);
        assertThat(result.pricingSchedule().basePrice()).isEqualByComparingTo("1000");
        ArgumentCaptor<Trip> captor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(result);
    }

    @Test
    void rejectsANonAdminCaller() {
        assertThatThrownBy(() -> service.createTrip(command(REGULAR_USER))).isInstanceOf(NotAnAdminException.class);
        verify(tripRepository, never()).save(any());
    }

    private static CreateTripCommand command(User requestedBy) {
        Instant now = Instant.now();
        return new CreateTripCommand(
                "Bali, Indonesia",
                "10 days across Ubud and Seminyak",
                LocalDate.now().plusMonths(3),
                LocalDate.now().plusMonths(3).plusDays(10),
                2,
                10,
                now.plus(21, ChronoUnit.DAYS),
                new BigDecimal("1000"),
                List.of(new PriceTier(5, new BigDecimal("800"))),
                List.of(),
                requestedBy);
    }
}
