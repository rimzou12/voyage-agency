package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.exception.TripNotFoundException;
import com.agencyvoyage.application.port.in.CreateGroupBookingCommand;
import com.agencyvoyage.application.port.out.GroupBookingEventPublisher;
import com.agencyvoyage.application.port.out.GroupBookingRepository;
import com.agencyvoyage.application.port.out.TripRepository;
import com.agencyvoyage.application.port.out.event.ParticipantJoinedEvent;
import com.agencyvoyage.domain.booking.GroupBooking;
import com.agencyvoyage.domain.trip.PriceTier;
import com.agencyvoyage.domain.trip.PricingSchedule;
import com.agencyvoyage.domain.trip.Trip;
import com.agencyvoyage.domain.trip.TripId;
import com.agencyvoyage.domain.user.User;
import com.agencyvoyage.domain.user.UserId;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateGroupBookingServiceTest {

    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");

    @Mock
    private TripRepository tripRepository;

    @Mock
    private GroupBookingRepository groupBookingRepository;

    @Mock
    private GroupBookingEventPublisher eventPublisher;

    private CreateGroupBookingService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new CreateGroupBookingService(tripRepository, groupBookingRepository, eventPublisher, clock);
    }

    @Test
    void createsAnOpenBookingWithTheCreatorAsFirstParticipantAndPublishesAnEvent() {
        Trip trip = trip();
        when(tripRepository.findById(trip.id())).thenReturn(Optional.of(trip));

        GroupBooking booking = service.createGroupBooking(new CreateGroupBookingCommand(trip.id(), alice()));

        assertThat(booking.currentParticipantCount()).isEqualTo(1);
        assertThat(booking.participants().get(0).customerName()).isEqualTo("Alice");

        verify(groupBookingRepository).save(booking);
        verify(eventPublisher).publishParticipantJoined(any(ParticipantJoinedEvent.class));
    }

    @Test
    void throwsWhenTheTripDoesNotExist() {
        TripId unknownId = TripId.newId();
        when(tripRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createGroupBooking(new CreateGroupBookingCommand(unknownId, alice())))
                .isInstanceOf(TripNotFoundException.class);
    }

    private static User alice() {
        return new User(UserId.newId(), "alice@example.com", "Alice");
    }

    private static Trip trip() {
        PricingSchedule schedule = PricingSchedule.of(
                new BigDecimal("1000"), List.of(new PriceTier(2, new BigDecimal("800"))), 5);
        return new Trip(
                TripId.newId(),
                "Bali",
                "10 days in Bali",
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 20),
                2,
                5,
                NOW.plus(1, ChronoUnit.DAYS),
                schedule,
                List.of());
    }
}
