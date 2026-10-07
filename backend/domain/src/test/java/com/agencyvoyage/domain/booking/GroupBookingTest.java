package com.agencyvoyage.domain.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agencyvoyage.domain.exception.AlreadyFinalizedException;
import com.agencyvoyage.domain.exception.AlreadyJoinedException;
import com.agencyvoyage.domain.exception.AlreadyWaitlistedException;
import com.agencyvoyage.domain.exception.BookingClosedException;
import com.agencyvoyage.domain.exception.BookingNotConfirmedException;
import com.agencyvoyage.domain.exception.BookingNotFullException;
import com.agencyvoyage.domain.exception.DeadlineExpiredException;
import com.agencyvoyage.domain.exception.FinalizationTooEarlyException;
import com.agencyvoyage.domain.exception.GroupFullException;
import com.agencyvoyage.domain.exception.HotelReservationAlreadyRequestedException;
import com.agencyvoyage.domain.exception.HotelReservationNotPendingException;
import com.agencyvoyage.domain.exception.InvalidReferralException;
import com.agencyvoyage.domain.exception.NotOnWaitlistException;
import com.agencyvoyage.domain.exception.ParticipantNotInBookingException;
import com.agencyvoyage.domain.trip.PriceTier;
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
import org.junit.jupiter.api.Test;

class GroupBookingTest {

    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");

    @Test
    void openingAddsTheCreatorAsTheFirstParticipant() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 2, 5);

        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);

        assertThat(booking.status()).isEqualTo(GroupBookingStatus.OPEN);
        assertThat(booking.currentParticipantCount()).isEqualTo(1);
        assertThat(booking.participants()).extracting(Participant::customerName).containsExactly("Alice");
    }

    @Test
    void cannotOpenAfterTheTripDeadlineHasPassed() {
        Trip trip = trip(NOW.minusSeconds(1), 2, 5);

        assertThatThrownBy(() -> GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW))
                .isInstanceOf(DeadlineExpiredException.class);
    }

    @Test
    void joiningAddsAParticipantAndDropsThePrice() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 2, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);
        assertThat(booking.currentPricePerSeat()).isEqualByComparingTo("1000");

        booking.join(participant("Bob", NOW), NOW);

        assertThat(booking.currentParticipantCount()).isEqualTo(2);
        assertThat(booking.currentPricePerSeat()).isEqualByComparingTo("800");
    }

    @Test
    void cannotJoinAFullGroup() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 1);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);

        assertThatThrownBy(() -> booking.join(participant("Bob", NOW), NOW))
                .isInstanceOf(GroupFullException.class);
    }

    @Test
    void cannotJoinAfterTheDeadline() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 2, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);

        Instant afterDeadline = NOW.plus(2, ChronoUnit.DAYS);
        assertThatThrownBy(() -> booking.join(participant("Bob", NOW), afterDeadline))
                .isInstanceOf(DeadlineExpiredException.class);
    }

    @Test
    void cannotJoinAClosedBooking() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);
        booking.finalizeBooking(NOW.plus(2, ChronoUnit.DAYS));

        assertThatThrownBy(() -> booking.join(participant("Bob", NOW), NOW.plus(2, ChronoUnit.DAYS)))
                .isInstanceOf(BookingClosedException.class);
    }

    @Test
    void cannotJoinTwiceAsTheSameUser() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 5);
        UserId bobId = UserId.newId();
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);
        booking.join(new Participant(ParticipantId.newId(), bobId, "Bob", NOW), NOW);

        assertThatThrownBy(() -> booking.join(new Participant(ParticipantId.newId(), bobId, "Bob", NOW), NOW))
                .isInstanceOf(AlreadyJoinedException.class);
    }

    @Test
    void joiningViaAReferralDiscountsBothTheReferrerAndTheReferred() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);
        ParticipantId aliceId = booking.participants().get(0).id();

        booking.join(new Participant(ParticipantId.newId(), UserId.newId(), "Bob", NOW, aliceId), NOW);
        ParticipantId bobId = booking.participants().get(1).id();

        BigDecimal tierPrice = booking.currentPricePerSeat();
        assertThat(booking.pricePerSeatFor(aliceId))
                .isEqualByComparingTo(tierPrice.subtract(GroupBooking.REFERRAL_DISCOUNT_PER_CREDIT));
        assertThat(booking.pricePerSeatFor(bobId))
                .isEqualByComparingTo(tierPrice.subtract(GroupBooking.REFERRAL_DISCOUNT_PER_CREDIT));
    }

    @Test
    void referralDiscountsStackForMultipleSuccessfulReferrals() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);
        ParticipantId aliceId = booking.participants().get(0).id();

        booking.join(new Participant(ParticipantId.newId(), UserId.newId(), "Bob", NOW, aliceId), NOW);
        booking.join(new Participant(ParticipantId.newId(), UserId.newId(), "Carol", NOW, aliceId), NOW);

        BigDecimal tierPrice = booking.currentPricePerSeat();
        assertThat(booking.pricePerSeatFor(aliceId))
                .isEqualByComparingTo(tierPrice.subtract(GroupBooking.REFERRAL_DISCOUNT_PER_CREDIT.multiply(new BigDecimal("2"))));
    }

    @Test
    void theReferralDiscountNeverTakesThePriceBelowZero() {
        PricingSchedule cheapSchedule = PricingSchedule.of(new BigDecimal("30"), List.of(), 2);
        Trip cheapTrip = new Trip(
                TripId.newId(),
                "Bali",
                "10 days in Bali",
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 20),
                1,
                2,
                NOW.plus(1, ChronoUnit.DAYS),
                cheapSchedule,
                List.of());
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), cheapTrip, participant("Alice", NOW), NOW);
        ParticipantId aliceId = booking.participants().get(0).id();

        booking.join(new Participant(ParticipantId.newId(), UserId.newId(), "Bob", NOW, aliceId), NOW);

        assertThat(booking.pricePerSeatFor(aliceId)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void anOrganicJoinerPaysTheFullTierPrice() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);
        booking.join(participant("Bob", NOW), NOW);
        ParticipantId bobId = booking.participants().get(1).id();

        assertThat(booking.pricePerSeatFor(bobId)).isEqualByComparingTo(booking.currentPricePerSeat());
    }

    @Test
    void cannotJoinWithAReferrerWhoIsNotActuallyAParticipant() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);
        ParticipantId strangerId = ParticipantId.newId();

        assertThatThrownBy(() -> booking.join(
                        new Participant(ParticipantId.newId(), UserId.newId(), "Bob", NOW, strangerId), NOW))
                .isInstanceOf(InvalidReferralException.class);
    }

    @Test
    void leavingRemovesTheParticipantAndReducesThePrice() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 2, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);
        Participant bob = participant("Bob", NOW);
        booking.join(bob, NOW);
        assertThat(booking.currentPricePerSeat()).isEqualByComparingTo("800");

        Optional<Participant> promoted = booking.leave(bob.userId(), NOW);

        assertThat(promoted).isEmpty();
        assertThat(booking.currentParticipantCount()).isEqualTo(1);
        assertThat(booking.participants()).extracting(Participant::customerName).containsExactly("Alice");
        assertThat(booking.currentPricePerSeat()).isEqualByComparingTo("1000");
    }

    @Test
    void cannotLeaveWithAnUnknownUserId() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 2, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);

        assertThatThrownBy(() -> booking.leave(UserId.newId(), NOW))
                .isInstanceOf(ParticipantNotInBookingException.class);
    }

    @Test
    void cannotLeaveAfterTheDeadline() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 2, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);
        Participant creator = booking.participants().get(0);

        assertThatThrownBy(() -> booking.leave(creator.userId(), NOW.plus(2, ChronoUnit.DAYS)))
                .isInstanceOf(DeadlineExpiredException.class);
    }

    @Test
    void cannotLeaveAClosedBooking() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);
        Participant creator = booking.participants().get(0);
        booking.finalizeBooking(NOW.plus(2, ChronoUnit.DAYS));

        assertThatThrownBy(() -> booking.leave(creator.userId(), NOW.plus(2, ChronoUnit.DAYS)))
                .isInstanceOf(BookingClosedException.class);
    }

    @Test
    void cannotJoinTheWaitlistWhileTheGroupStillHasRoom() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 2);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);

        assertThatThrownBy(() -> booking.joinWaitlist(waitlistEntry("Bob", NOW), NOW))
                .isInstanceOf(BookingNotFullException.class);
    }

    @Test
    void joinsTheWaitlistOnceTheGroupIsFull() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 1);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);

        booking.joinWaitlist(waitlistEntry("Bob", NOW), NOW);

        assertThat(booking.waitlist()).extracting(WaitlistEntry::customerName).containsExactly("Bob");
        assertThat(booking.currentParticipantCount()).isEqualTo(1);
    }

    @Test
    void cannotJoinTheWaitlistTwiceAsTheSameUser() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 1);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);
        UserId bobId = UserId.newId();
        booking.joinWaitlist(new WaitlistEntry(WaitlistEntryId.newId(), bobId, "Bob", NOW), NOW);

        assertThatThrownBy(
                        () -> booking.joinWaitlist(new WaitlistEntry(WaitlistEntryId.newId(), bobId, "Bob", NOW), NOW))
                .isInstanceOf(AlreadyWaitlistedException.class);
    }

    @Test
    void cannotJoinTheWaitlistIfAlreadyAParticipant() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 1);
        Participant alice = participant("Alice", NOW);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, alice, NOW);

        assertThatThrownBy(() -> booking.joinWaitlist(
                        new WaitlistEntry(WaitlistEntryId.newId(), alice.userId(), "Alice", NOW), NOW))
                .isInstanceOf(AlreadyJoinedException.class);
    }

    @Test
    void leavingPromotesTheLongestWaitingEntryIntoTheFreedSeat() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 1);
        Participant alice = participant("Alice", NOW);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, alice, NOW);
        booking.joinWaitlist(waitlistEntry("Bob", NOW), NOW);
        booking.joinWaitlist(waitlistEntry("Carol", NOW), NOW);

        Instant promotionTime = NOW.plus(1, ChronoUnit.HOURS);
        Optional<Participant> promoted = booking.leave(alice.userId(), promotionTime);

        assertThat(promoted).isPresent();
        assertThat(promoted.get().customerName()).isEqualTo("Bob");
        assertThat(promoted.get().joinedAt()).isEqualTo(promotionTime);
        assertThat(booking.participants()).extracting(Participant::customerName).containsExactly("Bob");
        assertThat(booking.waitlist()).extracting(WaitlistEntry::customerName).containsExactly("Carol");
    }

    @Test
    void leavingWithAnEmptyWaitlistPromotesNobody() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 2);
        Participant alice = participant("Alice", NOW);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, alice, NOW);

        assertThat(booking.leave(alice.userId(), NOW)).isEmpty();
    }

    @Test
    void leavingTheWaitlistRemovesTheEntry() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 1);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);
        UserId bobId = UserId.newId();
        booking.joinWaitlist(new WaitlistEntry(WaitlistEntryId.newId(), bobId, "Bob", NOW), NOW);

        booking.leaveWaitlist(bobId);

        assertThat(booking.waitlist()).isEmpty();
    }

    @Test
    void cannotLeaveTheWaitlistWithoutBeingOnIt() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 1);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);

        assertThatThrownBy(() -> booking.leaveWaitlist(UserId.newId()))
                .isInstanceOf(NotOnWaitlistException.class);
    }

    @Test
    void cannotRequestAHotelReservationBeforeTheBookingIsConfirmed() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);

        assertThatThrownBy(() -> booking.requestHotelReservation("REF-1", NOW))
                .isInstanceOf(BookingNotConfirmedException.class);
    }

    @Test
    void requestingAHotelReservationMarksItPending() {
        GroupBooking booking = confirmedBooking();

        booking.requestHotelReservation("REF-1", NOW);

        assertThat(booking.hotelReservationStatus()).isEqualTo(HotelReservationStatus.PENDING);
        assertThat(booking.hotelReservationReference()).isEqualTo("REF-1");
    }

    @Test
    void cannotRequestAHotelReservationTwice() {
        GroupBooking booking = confirmedBooking();
        booking.requestHotelReservation("REF-1", NOW);

        assertThatThrownBy(() -> booking.requestHotelReservation("REF-2", NOW))
                .isInstanceOf(HotelReservationAlreadyRequestedException.class);
    }

    @Test
    void confirmingAPendingHotelReservationMarksItConfirmed() {
        GroupBooking booking = confirmedBooking();
        booking.requestHotelReservation("REF-1", NOW);

        booking.confirmHotelReservation(NOW);

        assertThat(booking.hotelReservationStatus()).isEqualTo(HotelReservationStatus.CONFIRMED);
    }

    @Test
    void cannotConfirmAHotelReservationThatWasNeverRequested() {
        GroupBooking booking = confirmedBooking();

        assertThatThrownBy(() -> booking.confirmHotelReservation(NOW))
                .isInstanceOf(HotelReservationNotPendingException.class);
    }

    @Test
    void cannotConfirmAHotelReservationTwice() {
        GroupBooking booking = confirmedBooking();
        booking.requestHotelReservation("REF-1", NOW);
        booking.confirmHotelReservation(NOW);

        assertThatThrownBy(() -> booking.confirmHotelReservation(NOW))
                .isInstanceOf(HotelReservationNotPendingException.class);
    }

    @Test
    void finalizeConfirmsWhenMinimumParticipationIsReached() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 2, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);
        booking.join(participant("Bob", NOW), NOW);

        GroupBookingStatus result = booking.finalizeBooking(NOW.plus(2, ChronoUnit.DAYS));

        assertThat(result).isEqualTo(GroupBookingStatus.CONFIRMED);
        assertThat(booking.status()).isEqualTo(GroupBookingStatus.CONFIRMED);
    }

    @Test
    void finalizeCancelsWhenMinimumParticipationIsNotReached() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 3, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);

        GroupBookingStatus result = booking.finalizeBooking(NOW.plus(2, ChronoUnit.DAYS));

        assertThat(result).isEqualTo(GroupBookingStatus.CANCELLED);
    }

    @Test
    void cannotFinalizeBeforeTheDeadline() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 2, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);

        assertThatThrownBy(() -> booking.finalizeBooking(NOW))
                .isInstanceOf(FinalizationTooEarlyException.class);
    }

    @Test
    void cannotFinalizeTwice() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);
        booking.finalizeBooking(NOW.plus(2, ChronoUnit.DAYS));

        assertThatThrownBy(() -> booking.finalizeBooking(NOW.plus(3, ChronoUnit.DAYS)))
                .isInstanceOf(AlreadyFinalizedException.class);
    }

    private static Trip trip(Instant deadline, int minParticipants, int maxParticipants) {
        List<PriceTier> tiers = maxParticipants >= 2
                ? List.of(new PriceTier(2, new BigDecimal("800")))
                : List.of();
        PricingSchedule schedule = PricingSchedule.of(new BigDecimal("1000"), tiers, maxParticipants);
        return new Trip(
                TripId.newId(),
                "Bali",
                "10 days in Bali",
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 20),
                minParticipants,
                maxParticipants,
                deadline,
                schedule,
                List.of());
    }

    private static Participant participant(String name, Instant joinedAt) {
        return new Participant(ParticipantId.newId(), UserId.newId(), name, joinedAt);
    }

    private static WaitlistEntry waitlistEntry(String name, Instant joinedAt) {
        return new WaitlistEntry(WaitlistEntryId.newId(), UserId.newId(), name, joinedAt);
    }

    private static GroupBooking confirmedBooking() {
        Trip trip = trip(NOW.plus(1, ChronoUnit.DAYS), 1, 5);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", NOW), NOW);
        booking.finalizeBooking(NOW.plus(2, ChronoUnit.DAYS));
        return booking;
    }
}
