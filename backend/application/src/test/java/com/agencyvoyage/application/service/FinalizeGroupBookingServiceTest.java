package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.port.out.GroupBookingEventPublisher;
import com.agencyvoyage.application.port.out.GroupBookingRepository;
import com.agencyvoyage.application.port.out.event.GroupBookingFinalizedEvent;
import com.agencyvoyage.domain.booking.GroupBooking;
import com.agencyvoyage.domain.booking.GroupBookingId;
import com.agencyvoyage.domain.booking.GroupBookingStatus;
import com.agencyvoyage.domain.booking.Participant;
import com.agencyvoyage.domain.booking.ParticipantId;
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
class FinalizeGroupBookingServiceTest {

    private static final Instant DEADLINE = Instant.parse("2027-01-01T00:00:00Z");
    private static final Instant AFTER_DEADLINE = DEADLINE.plus(1, ChronoUnit.HOURS);

    @Mock
    private GroupBookingRepository groupBookingRepository;

    @Mock
    private GroupBookingEventPublisher eventPublisher;

    private FinalizeGroupBookingService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AFTER_DEADLINE, ZoneOffset.UTC);
        service = new FinalizeGroupBookingService(groupBookingRepository, eventPublisher, clock);
    }

    @Test
    void confirmsAndPublishesWhenMinimumParticipationWasReached() {
        GroupBooking booking = openBooking(1, 5);
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        GroupBookingStatus status = service.finalizeGroupBooking(booking.id());

        assertThat(status).isEqualTo(GroupBookingStatus.CONFIRMED);
        verify(groupBookingRepository).save(booking);
        verify(eventPublisher).publishFinalized(any(GroupBookingFinalizedEvent.class));
    }

    @Test
    void cancelsWhenMinimumParticipationWasNotReached() {
        GroupBooking booking = openBooking(3, 5);
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        GroupBookingStatus status = service.finalizeGroupBooking(booking.id());

        assertThat(status).isEqualTo(GroupBookingStatus.CANCELLED);
    }

    @Test
    void finalizeAllDueProcessesEveryCandidateFromTheRepository() {
        GroupBooking a = openBooking(1, 5);
        GroupBooking b = openBooking(3, 5);
        when(groupBookingRepository.findOpenWithDeadlineAtOrBefore(AFTER_DEADLINE)).thenReturn(List.of(a, b));

        List<GroupBookingId> finalizedIds = service.finalizeAllDue(AFTER_DEADLINE);

        assertThat(finalizedIds).containsExactlyInAnyOrder(a.id(), b.id());
        assertThat(a.status()).isEqualTo(GroupBookingStatus.CONFIRMED);
        assertThat(b.status()).isEqualTo(GroupBookingStatus.CANCELLED);
        verify(groupBookingRepository).save(a);
        verify(groupBookingRepository).save(b);
    }

    private static GroupBooking openBooking(int minParticipants, int maxParticipants) {
        PricingSchedule schedule = PricingSchedule.of(new BigDecimal("1000"), List.of(), maxParticipants);
        Trip trip = new Trip(
                TripId.newId(),
                "Bali",
                "10 days in Bali",
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 20),
                minParticipants,
                maxParticipants,
                DEADLINE,
                schedule,
                List.of());
        Participant creator =
                new Participant(ParticipantId.newId(), UserId.newId(), "Alice", DEADLINE.minusSeconds(60));
        return GroupBooking.open(GroupBookingId.newId(), trip, creator, DEADLINE.minusSeconds(60));
    }
}
