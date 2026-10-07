package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.exception.GroupBookingNotFoundException;
import com.agencyvoyage.application.port.in.LeaveWaitlistCommand;
import com.agencyvoyage.application.port.out.GroupBookingRepository;
import com.agencyvoyage.domain.booking.GroupBooking;
import com.agencyvoyage.domain.booking.GroupBookingId;
import com.agencyvoyage.domain.booking.Participant;
import com.agencyvoyage.domain.booking.ParticipantId;
import com.agencyvoyage.domain.booking.WaitlistEntry;
import com.agencyvoyage.domain.booking.WaitlistEntryId;
import com.agencyvoyage.domain.exception.NotOnWaitlistException;
import com.agencyvoyage.domain.trip.PricingSchedule;
import com.agencyvoyage.domain.trip.Trip;
import com.agencyvoyage.domain.trip.TripId;
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
class LeaveWaitlistServiceTest {

    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");

    @Mock
    private GroupBookingRepository groupBookingRepository;

    private LeaveWaitlistService service;

    @BeforeEach
    void setUp() {
        service = new LeaveWaitlistService(groupBookingRepository);
    }

    @Test
    void removesTheCallerFromTheWaitlist() {
        GroupBooking booking = openFullBooking();
        UserId bobId = UserId.newId();
        booking.joinWaitlist(new WaitlistEntry(WaitlistEntryId.newId(), bobId, "Bob", NOW), NOW);
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        GroupBooking result = service.leaveWaitlist(new LeaveWaitlistCommand(booking.id(), bobId));

        assertThat(result.waitlist()).isEmpty();
        verify(groupBookingRepository).save(booking);
    }

    @Test
    void throwsWhenTheBookingDoesNotExist() {
        GroupBookingId unknownId = GroupBookingId.newId();
        when(groupBookingRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> service.leaveWaitlist(new LeaveWaitlistCommand(unknownId, UserId.newId())))
                .isInstanceOf(GroupBookingNotFoundException.class);
    }

    @Test
    void propagatesDomainRuleViolationsLikeNotBeingOnTheWaitlist() {
        GroupBooking booking = openFullBooking();
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.leaveWaitlist(new LeaveWaitlistCommand(booking.id(), UserId.newId())))
                .isInstanceOf(NotOnWaitlistException.class);
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
}
