package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.exception.GroupBookingNotFoundException;
import com.agencyvoyage.application.port.in.RequestHotelReservationCommand;
import com.agencyvoyage.application.port.out.EmailSender;
import com.agencyvoyage.application.port.out.GroupBookingRepository;
import com.agencyvoyage.application.port.out.UserRepository;
import com.agencyvoyage.domain.booking.GroupBooking;
import com.agencyvoyage.domain.booking.GroupBookingId;
import com.agencyvoyage.domain.booking.HotelReservationStatus;
import com.agencyvoyage.domain.booking.Participant;
import com.agencyvoyage.domain.booking.ParticipantId;
import com.agencyvoyage.domain.exception.BookingNotConfirmedException;
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
class RequestHotelReservationServiceTest {

    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");

    @Mock
    private GroupBookingRepository groupBookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailSender emailSender;

    private RequestHotelReservationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new RequestHotelReservationService(groupBookingRepository, userRepository, emailSender, clock);
    }

    @Test
    void marksTheReservationPendingAndEmailsEveryParticipant() {
        UserId aliceId = UserId.newId();
        GroupBooking booking = confirmedBooking(aliceId);
        User alice = new User(aliceId, "alice@example.com", "Alice");
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));
        when(userRepository.findById(aliceId)).thenReturn(Optional.of(alice));

        GroupBooking result =
                service.requestHotelReservation(new RequestHotelReservationCommand(booking.id(), "REF-1"));

        assertThat(result.hotelReservationStatus()).isEqualTo(HotelReservationStatus.PENDING);
        assertThat(result.hotelReservationReference()).isEqualTo("REF-1");
        verify(groupBookingRepository).save(booking);
        verify(emailSender).sendHotelReservationRequested("alice@example.com", "Alice", booking.id(), "REF-1");
    }

    @Test
    void skipsEmailingAParticipantWhoseUserRecordIsMissing() {
        UserId aliceId = UserId.newId();
        GroupBooking booking = confirmedBooking(aliceId);
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));
        when(userRepository.findById(aliceId)).thenReturn(Optional.empty());

        service.requestHotelReservation(new RequestHotelReservationCommand(booking.id(), "REF-1"));

        verify(emailSender, never()).sendHotelReservationRequested(any(), any(), any(), any());
    }

    @Test
    void throwsWhenTheBookingDoesNotExist() {
        GroupBookingId unknownId = GroupBookingId.newId();
        when(groupBookingRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestHotelReservation(new RequestHotelReservationCommand(unknownId, "REF-1")))
                .isInstanceOf(GroupBookingNotFoundException.class);
    }

    @Test
    void propagatesDomainRuleViolationsLikeANotYetConfirmedBooking() {
        Trip trip = trip();
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice"), NOW);
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.requestHotelReservation(new RequestHotelReservationCommand(booking.id(), "REF-1")))
                .isInstanceOf(BookingNotConfirmedException.class);
    }

    private static GroupBooking confirmedBooking(UserId creatorUserId) {
        Trip trip = trip();
        GroupBooking booking = GroupBooking.open(
                GroupBookingId.newId(), trip, new Participant(ParticipantId.newId(), creatorUserId, "Alice", NOW), NOW);
        booking.finalizeBooking(NOW.plus(2, ChronoUnit.DAYS));
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
