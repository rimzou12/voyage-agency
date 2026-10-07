package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.exception.GroupBookingNotFoundException;
import com.agencyvoyage.application.port.in.LeaveGroupBookingCommand;
import com.agencyvoyage.application.port.out.GroupBookingEventPublisher;
import com.agencyvoyage.application.port.out.GroupBookingRepository;
import com.agencyvoyage.application.port.out.event.ParticipantJoinedEvent;
import com.agencyvoyage.application.port.out.event.ParticipantLeftEvent;
import com.agencyvoyage.domain.booking.GroupBooking;
import com.agencyvoyage.domain.booking.GroupBookingId;
import com.agencyvoyage.domain.booking.Participant;
import com.agencyvoyage.domain.booking.ParticipantId;
import com.agencyvoyage.domain.booking.WaitlistEntry;
import com.agencyvoyage.domain.booking.WaitlistEntryId;
import com.agencyvoyage.domain.exception.ParticipantNotInBookingException;
import com.agencyvoyage.domain.trip.PriceTier;
import com.agencyvoyage.domain.trip.PricingSchedule;
import com.agencyvoyage.domain.trip.Trip;
import com.agencyvoyage.domain.trip.TripId;
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
class LeaveGroupBookingServiceTest {

    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");

    @Mock
    private GroupBookingRepository groupBookingRepository;

    @Mock
    private GroupBookingEventPublisher eventPublisher;

    private LeaveGroupBookingService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new LeaveGroupBookingService(groupBookingRepository, eventPublisher, clock);
    }

    @Test
    void removesTheParticipantAndPublishesAnEvent() {
        GroupBooking booking = openBooking();
        UserId bobId = UserId.newId();
        booking.join(new Participant(ParticipantId.newId(), bobId, "Bob", NOW), NOW);
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        GroupBooking result = service.leaveGroupBooking(new LeaveGroupBookingCommand(booking.id(), bobId));

        assertThat(result.currentParticipantCount()).isEqualTo(1);
        verify(groupBookingRepository).save(booking);
        verify(eventPublisher).publishParticipantLeft(any(ParticipantLeftEvent.class));
        verify(eventPublisher, never()).publishParticipantJoined(any());
    }

    @Test
    void promotesTheWaitlistedEntryAndPublishesAJoinedEventForThem() {
        GroupBooking booking = openFullBookingWithOneWaitlisted();
        UserId aliceId = booking.participants().get(0).userId();
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        GroupBooking result = service.leaveGroupBooking(new LeaveGroupBookingCommand(booking.id(), aliceId));

        assertThat(result.currentParticipantCount()).isEqualTo(1);
        assertThat(result.participants()).extracting(Participant::customerName).containsExactly("Bob");
        assertThat(result.waitlist()).isEmpty();
        verify(eventPublisher).publishParticipantLeft(any(ParticipantLeftEvent.class));
        verify(eventPublisher).publishParticipantJoined(any(ParticipantJoinedEvent.class));
    }

    @Test
    void throwsWhenTheBookingDoesNotExist() {
        GroupBookingId unknownId = GroupBookingId.newId();
        when(groupBookingRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.leaveGroupBooking(
                        new LeaveGroupBookingCommand(unknownId, UserId.newId())))
                .isInstanceOf(GroupBookingNotFoundException.class);
    }

    @Test
    void propagatesDomainRuleViolationsLikeAnUnknownParticipant() {
        GroupBooking booking = openBooking();
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.leaveGroupBooking(
                        new LeaveGroupBookingCommand(booking.id(), UserId.newId())))
                .isInstanceOf(ParticipantNotInBookingException.class);
    }

    private static GroupBooking openBooking() {
        PricingSchedule schedule = PricingSchedule.of(
                new BigDecimal("1000"), List.of(new PriceTier(2, new BigDecimal("800"))), 5);
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

    private static GroupBooking openFullBookingWithOneWaitlisted() {
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
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, creator, NOW);
        booking.joinWaitlist(new WaitlistEntry(WaitlistEntryId.newId(), UserId.newId(), "Bob", NOW), NOW);
        return booking;
    }
}
