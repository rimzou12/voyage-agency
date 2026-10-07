package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.exception.GroupBookingNotFoundException;
import com.agencyvoyage.application.port.in.JoinWaitlistCommand;
import com.agencyvoyage.application.port.out.GroupBookingRepository;
import com.agencyvoyage.domain.booking.GroupBooking;
import com.agencyvoyage.domain.booking.GroupBookingId;
import com.agencyvoyage.domain.booking.Participant;
import com.agencyvoyage.domain.booking.ParticipantId;
import com.agencyvoyage.domain.exception.BookingNotFullException;
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
class JoinWaitlistServiceTest {

    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");

    @Mock
    private GroupBookingRepository groupBookingRepository;

    private JoinWaitlistService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new JoinWaitlistService(groupBookingRepository, clock);
    }

    @Test
    void addsTheCallerToTheWaitlistOfAFullBooking() {
        GroupBooking booking = openFullBooking();
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        GroupBooking result = service.joinWaitlist(new JoinWaitlistCommand(booking.id(), bob()));

        assertThat(result.waitlist()).hasSize(1);
        assertThat(result.waitlist().get(0).customerName()).isEqualTo("Bob");
        verify(groupBookingRepository).save(booking);
    }

    @Test
    void throwsWhenTheBookingDoesNotExist() {
        GroupBookingId unknownId = GroupBookingId.newId();
        when(groupBookingRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.joinWaitlist(new JoinWaitlistCommand(unknownId, bob())))
                .isInstanceOf(GroupBookingNotFoundException.class);
    }

    @Test
    void propagatesDomainRuleViolationsLikeANotYetFullBooking() {
        GroupBooking booking = openBookingWithRoom();
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.joinWaitlist(new JoinWaitlistCommand(booking.id(), bob())))
                .isInstanceOf(BookingNotFullException.class);
    }

    private static User bob() {
        return new User(UserId.newId(), "bob@example.com", "Bob");
    }

    private static GroupBooking openFullBooking() {
        PricingSchedule schedule = PricingSchedule.of(new BigDecimal("1000"), List.of(), 1);
        Trip trip = new Trip(
                TripId.newId(),
                "Bali",
                "10 days in Bali",
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 20),
                1,
                1,
                NOW.plus(1, ChronoUnit.DAYS),
                schedule,
                List.of());
        Participant creator = new Participant(ParticipantId.newId(), UserId.newId(), "Alice", NOW);
        return GroupBooking.open(GroupBookingId.newId(), trip, creator, NOW);
    }

    private static GroupBooking openBookingWithRoom() {
        PricingSchedule schedule = PricingSchedule.of(new BigDecimal("1000"), List.of(), 5);
        Trip trip = new Trip(
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
        Participant creator = new Participant(ParticipantId.newId(), UserId.newId(), "Alice", NOW);
        return GroupBooking.open(GroupBookingId.newId(), trip, creator, NOW);
    }
}
