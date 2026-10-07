package com.agencyvoyage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agencyvoyage.application.exception.GroupBookingNotFoundException;
import com.agencyvoyage.application.port.in.JoinGroupBookingCommand;
import com.agencyvoyage.application.port.out.EmailSender;
import com.agencyvoyage.application.port.out.GroupBookingEventPublisher;
import com.agencyvoyage.application.port.out.GroupBookingRepository;
import com.agencyvoyage.application.port.out.event.ParticipantJoinedEvent;
import com.agencyvoyage.domain.booking.GroupBooking;
import com.agencyvoyage.domain.booking.GroupBookingId;
import com.agencyvoyage.domain.booking.Participant;
import com.agencyvoyage.domain.booking.ParticipantId;
import com.agencyvoyage.domain.exception.GroupFullException;
import com.agencyvoyage.domain.exception.InvalidReferralException;
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
class JoinGroupBookingServiceTest {

    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");

    @Mock
    private GroupBookingRepository groupBookingRepository;

    @Mock
    private GroupBookingEventPublisher eventPublisher;

    @Mock
    private EmailSender emailSender;

    private JoinGroupBookingService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new JoinGroupBookingService(groupBookingRepository, eventPublisher, emailSender, clock);
    }

    @Test
    void addsAParticipantAndPublishesAnEventWithTheNewPrice() {
        GroupBooking booking = openBooking(2, 5);
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        GroupBooking result = service.joinGroupBooking(new JoinGroupBookingCommand(booking.id(), bob()));

        assertThat(result.currentParticipantCount()).isEqualTo(2);
        assertThat(result.currentPricePerSeat()).isEqualByComparingTo("800");
        verify(groupBookingRepository).save(booking);
        verify(eventPublisher).publishParticipantJoined(any(ParticipantJoinedEvent.class));
    }

    @Test
    void emailsTheJoinerOnceTheyveJoined() {
        GroupBooking booking = openBooking(2, 5);
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        service.joinGroupBooking(new JoinGroupBookingCommand(booking.id(), bob()));

        verify(emailSender).sendGroupBookingJoined("bob@example.com", "Bob", booking.id(), 2, new BigDecimal("800"));
    }

    @Test
    void joiningWithAReferrerDiscountsBothParticipants() {
        GroupBooking booking = openBooking(2, 5);
        ParticipantId aliceId = booking.participants().get(0).id();
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        GroupBooking result =
                service.joinGroupBooking(new JoinGroupBookingCommand(booking.id(), bob(), aliceId));

        ParticipantId bobId = result.participants().get(1).id();
        assertThat(result.pricePerSeatFor(aliceId))
                .isEqualByComparingTo(result.currentPricePerSeat().subtract(GroupBooking.REFERRAL_DISCOUNT_PER_CREDIT));
        assertThat(result.pricePerSeatFor(bobId))
                .isEqualByComparingTo(result.currentPricePerSeat().subtract(GroupBooking.REFERRAL_DISCOUNT_PER_CREDIT));
    }

    @Test
    void propagatesDomainRuleViolationsLikeAnInvalidReferrer() {
        GroupBooking booking = openBooking(2, 5);
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.joinGroupBooking(
                        new JoinGroupBookingCommand(booking.id(), bob(), ParticipantId.newId())))
                .isInstanceOf(InvalidReferralException.class);
    }

    @Test
    void throwsWhenTheBookingDoesNotExist() {
        GroupBookingId unknownId = GroupBookingId.newId();
        when(groupBookingRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.joinGroupBooking(new JoinGroupBookingCommand(unknownId, bob())))
                .isInstanceOf(GroupBookingNotFoundException.class);
    }

    @Test
    void propagatesDomainRuleViolationsLikeAFullGroup() {
        GroupBooking booking = openBooking(1, 1);
        when(groupBookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.joinGroupBooking(new JoinGroupBookingCommand(booking.id(), bob())))
                .isInstanceOf(GroupFullException.class);
    }

    private static User bob() {
        return new User(UserId.newId(), "bob@example.com", "Bob");
    }

    private static GroupBooking openBooking(int minParticipants, int maxParticipants) {
        List<PriceTier> tiers = maxParticipants >= 2
                ? List.of(new PriceTier(2, new BigDecimal("800")))
                : List.of();
        PricingSchedule schedule = PricingSchedule.of(new BigDecimal("1000"), tiers, maxParticipants);
        Trip trip = new Trip(
                TripId.newId(),
                "Bali",
                "10 days in Bali",
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 20),
                minParticipants,
                maxParticipants,
                NOW.plus(1, ChronoUnit.DAYS),
                schedule,
                List.of());
        Participant creator = new Participant(ParticipantId.newId(), UserId.newId(), "Alice", NOW);
        return GroupBooking.open(GroupBookingId.newId(), trip, creator, NOW);
    }
}
