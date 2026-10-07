package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.exception.GroupBookingNotFoundException;
import com.agencyvoyage.application.port.in.ConfirmHotelReservationCommand;
import com.agencyvoyage.application.port.out.EmailSender;
import com.agencyvoyage.application.port.out.GroupBookingRepository;
import com.agencyvoyage.application.port.out.UserRepository;
import com.agencyvoyage.domain.booking.GroupBooking;
import com.agencyvoyage.domain.booking.GroupBookingId;
import com.agencyvoyage.domain.booking.HotelReservationStatus;
import com.agencyvoyage.domain.booking.Participant;
import com.agencyvoyage.domain.booking.ParticipantId;
import com.agencyvoyage.domain.exception.HotelReservationNotPendingException;
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
class ConfirmHotelReservationServiceTest {

    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");

    @Mock
    private GroupBookingRepository groupBookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailSender emailSender;

    private ConfirmHotelReservationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new ConfirmHotelReservationService(groupBookingRepository, userRepository, emailSender, clock);
    }

    @Test
    void confirmsAndEmailsEveryParticipant() {
        UserId aliceId = UserId.newId();
        GroupBooking booking = pendingBooking(aliceId);
        User alice = new User(aliceId, "alice@example.com", "Alice");
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));
        when(userRepository.findById(aliceId)).thenReturn(Optional.of(alice));

        GroupBooking result = service.confirmHotelReservation(new ConfirmHotelReservationCommand(booking.id()));

        assertThat(result.hotelReservationStatus()).isEqualTo(HotelReservationStatus.CONFIRMED);
        verify(groupBookingRepository).save(booking);
        verify(emailSender).sendHotelReservationConfirmed("alice@example.com", "Alice", booking.id(), "REF-1");
    }

    @Test
    void skipsEmailingAParticipantWhoseUserRecordIsMissing() {
        UserId aliceId = UserId.newId();
        GroupBooking booking = pendingBooking(aliceId);
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));
        when(userRepository.findById(aliceId)).thenReturn(Optional.empty());

        service.confirmHotelReservation(new ConfirmHotelReservationCommand(booking.id()));

        verify(emailSender, never()).sendHotelReservationConfirmed(any(), any(), any(), any());
    }

    @Test
    void throwsWhenTheBookingDoesNotExist() {
        GroupBookingId unknownId = GroupBookingId.newId();
        when(groupBookingRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmHotelReservation(new ConfirmHotelReservationCommand(unknownId)))
                .isInstanceOf(GroupBookingNotFoundException.class);
    }

    @Test
    void propagatesDomainRuleViolationsLikeANeverRequestedReservation() {
        Trip trip = trip();
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice"), NOW);
        booking.finalizeBooking(NOW.plus(2, ChronoUnit.DAYS));
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.confirmHotelReservation(new ConfirmHotelReservationCommand(booking.id())))
                .isInstanceOf(HotelReservationNotPendingException.class);
    }

    private static GroupBooking pendingBooking(UserId creatorUserId) {
        Trip trip = trip();
        GroupBooking booking =
                GroupBooking.open(GroupBookingId.newId(), trip, new Participant(ParticipantId.newId(), creatorUserId, "Alice", NOW), NOW);
        booking.finalizeBooking(NOW.plus(2, ChronoUnit.DAYS));
        booking.requestHotelReservation("REF-1", NOW);
        return booking;
    }

    private static Trip trip() {
        PricingSchedule schedule = PricingSchedule.of(new BigDecimal("1000"), List.of(), 5);
        return new Trip(
                TripId.newId(),
                "Bali",
                "10 days in Bali",
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 20),
                1,
                5,
                NOW.plus(1, ChronoUnit.DAYS),
                schedule,
                List.of());
    }

    private static Participant participant(String name) {
        return new Participant(ParticipantId.newId(), UserId.newId(), name, NOW);
    }
}
