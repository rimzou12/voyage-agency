package com.agencyvoyage.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.agencyvoyage.domain.booking.GroupBooking;
import com.agencyvoyage.domain.booking.GroupBookingId;
import com.agencyvoyage.domain.booking.GroupBookingStatus;
import com.agencyvoyage.domain.booking.HotelReservationStatus;
import com.agencyvoyage.domain.booking.Participant;
import com.agencyvoyage.domain.booking.ParticipantId;
import com.agencyvoyage.domain.booking.WaitlistEntry;
import com.agencyvoyage.domain.booking.WaitlistEntryId;
import com.agencyvoyage.domain.trip.PriceTier;
import com.agencyvoyage.domain.trip.PricingSchedule;
import com.agencyvoyage.domain.trip.Trip;
import com.agencyvoyage.domain.trip.TripId;
import com.agencyvoyage.domain.user.UserId;
import com.agencyvoyage.infrastructure.config.AbstractPostgresIT;
import com.agencyvoyage.infrastructure.persistence.jpa.adapter.GroupBookingRepositoryAdapter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GroupBookingRepositoryAdapterIT extends AbstractPostgresIT {

    @Autowired
    private GroupBookingRepositoryAdapter adapter;

    @Test
    void savesAndReloadsANewlyOpenedBooking() {
        Trip trip = trip(Instant.now().plus(1, ChronoUnit.DAYS));
        GroupBooking booking =
                GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", Instant.now()), Instant.now());

        adapter.save(booking);

        Optional<GroupBooking> reloaded = adapter.findById(booking.id());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().currentParticipantCount()).isEqualTo(1);
        assertThat(reloaded.get().participants().get(0).customerName()).isEqualTo("Alice");
        assertThat(reloaded.get().status()).isEqualTo(GroupBookingStatus.OPEN);
    }

    @Test
    void persistsParticipantsAddedAfterTheInitialSave() {
        Trip trip = trip(Instant.now().plus(1, ChronoUnit.DAYS));
        Instant now = Instant.now();
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", now), now);
        adapter.save(booking);

        booking.join(participant("Bob", now), now);
        adapter.save(booking);

        GroupBooking reloaded = adapter.findById(booking.id()).orElseThrow();
        assertThat(reloaded.currentParticipantCount()).isEqualTo(2);
        assertThat(reloaded.currentPricePerSeat()).isEqualByComparingTo("800");
    }

    @Test
    void persistsAndReloadsAReferralDiscount() {
        Trip trip = trip(Instant.now().plus(1, ChronoUnit.DAYS));
        Instant now = Instant.now();
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", now), now);
        ParticipantId aliceId = booking.participants().get(0).id();
        adapter.save(booking);

        booking.join(new Participant(ParticipantId.newId(), UserId.newId(), "Bob", now, aliceId), now);
        adapter.save(booking);

        GroupBooking reloaded = adapter.findById(booking.id()).orElseThrow();
        ParticipantId bobId = reloaded.participants().get(1).id();
        assertThat(reloaded.participants().get(1).referredBy()).isEqualTo(aliceId);
        assertThat(reloaded.pricePerSeatFor(aliceId))
                .isEqualByComparingTo(reloaded.currentPricePerSeat().subtract(GroupBooking.REFERRAL_DISCOUNT_PER_CREDIT));
        assertThat(reloaded.pricePerSeatFor(bobId))
                .isEqualByComparingTo(reloaded.currentPricePerSeat().subtract(GroupBooking.REFERRAL_DISCOUNT_PER_CREDIT));
    }

    @Test
    void persistsAndReloadsAHotelReservation() {
        Trip trip = fullTrip(Instant.now().plus(1, ChronoUnit.DAYS));
        Instant now = Instant.now();
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", now), now);
        adapter.save(booking);
        booking.finalizeBooking(now.plus(2, ChronoUnit.DAYS));
        adapter.save(booking);

        booking.requestHotelReservation("REF-123", now);
        adapter.save(booking);

        GroupBooking reloadedAfterRequest = adapter.findById(booking.id()).orElseThrow();
        assertThat(reloadedAfterRequest.hotelReservationStatus()).isEqualTo(HotelReservationStatus.PENDING);
        assertThat(reloadedAfterRequest.hotelReservationReference()).isEqualTo("REF-123");

        reloadedAfterRequest.confirmHotelReservation(now);
        adapter.save(reloadedAfterRequest);

        GroupBooking reloadedAfterConfirm = adapter.findById(booking.id()).orElseThrow();
        assertThat(reloadedAfterConfirm.hotelReservationStatus()).isEqualTo(HotelReservationStatus.CONFIRMED);
        assertThat(reloadedAfterConfirm.hotelReservationReference()).isEqualTo("REF-123");
    }

    @Test
    void persistsAParticipantLeavingAfterTheInitialSave() {
        Trip trip = trip(Instant.now().plus(1, ChronoUnit.DAYS));
        Instant now = Instant.now();
        UserId bobUserId = UserId.newId();
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", now), now);
        booking.join(new Participant(ParticipantId.newId(), bobUserId, "Bob", now), now);
        adapter.save(booking);

        booking.leave(bobUserId, now);
        adapter.save(booking);

        GroupBooking reloaded = adapter.findById(booking.id()).orElseThrow();
        assertThat(reloaded.currentParticipantCount()).isEqualTo(1);
        assertThat(reloaded.participants()).extracting(Participant::customerName).containsExactly("Alice");
        assertThat(reloaded.currentPricePerSeat()).isEqualByComparingTo("1000");
    }

    @Test
    void persistsAWaitlistEntryAddedAfterTheInitialSave() {
        Trip trip = fullTrip(Instant.now().plus(1, ChronoUnit.DAYS));
        Instant now = Instant.now();
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", now), now);
        adapter.save(booking);

        booking.joinWaitlist(new WaitlistEntry(WaitlistEntryId.newId(), UserId.newId(), "Bob", now), now);
        adapter.save(booking);

        GroupBooking reloaded = adapter.findById(booking.id()).orElseThrow();
        assertThat(reloaded.waitlist()).extracting(WaitlistEntry::customerName).containsExactly("Bob");
    }

    @Test
    void persistsAWaitlistPromotionWhenAParticipantLeaves() {
        Trip trip = fullTrip(Instant.now().plus(1, ChronoUnit.DAYS));
        Instant now = Instant.now();
        Participant alice = participant("Alice", now);
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, alice, now);
        booking.joinWaitlist(new WaitlistEntry(WaitlistEntryId.newId(), UserId.newId(), "Bob", now), now);
        adapter.save(booking);

        booking.leave(alice.userId(), now);
        adapter.save(booking);

        GroupBooking reloaded = adapter.findById(booking.id()).orElseThrow();
        assertThat(reloaded.participants()).extracting(Participant::customerName).containsExactly("Bob");
        assertThat(reloaded.waitlist()).isEmpty();
    }

    @Test
    void findsOpenBookingsWithDeadlineAtOrBeforeTheGivenInstant() {
        Instant deadline = Instant.now().plus(1, ChronoUnit.SECONDS);
        Trip trip = trip(deadline);
        Instant now = Instant.now();
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", now), now);
        adapter.save(booking);

        List<GroupBooking> due = adapter.findOpenWithDeadlineAtOrBefore(deadline.plusSeconds(1));

        assertThat(due).extracting(GroupBooking::id).contains(booking.id());
    }

    @Test
    void findByTripIdReturnsOnlyThatTripsBookings() {
        Trip trip = trip(Instant.now().plus(1, ChronoUnit.DAYS));
        Instant now = Instant.now();
        GroupBooking booking = GroupBooking.open(GroupBookingId.newId(), trip, participant("Alice", now), now);
        adapter.save(booking);

        assertThat(adapter.findByTripId(trip.id())).extracting(GroupBooking::id).containsExactly(booking.id());
        assertThat(adapter.findByTripId(TripId.newId())).isEmpty();
    }

    private static Participant participant(String name, Instant joinedAt) {
        return new Participant(ParticipantId.newId(), UserId.newId(), name, joinedAt);
    }

    private static Trip trip(Instant deadline) {
        PricingSchedule schedule = PricingSchedule.of(
                new BigDecimal("1000"), List.of(new PriceTier(2, new BigDecimal("800"))), 10);
        return new Trip(
                TripId.newId(),
                "Bali",
                "10 days in Bali",
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 20),
                2,
                10,
                deadline,
                schedule,
                List.of());
    }

    private static Trip fullTrip(Instant deadline) {
        PricingSchedule schedule = PricingSchedule.of(new BigDecimal("1000"), List.of(), 1);
        return new Trip(
                TripId.newId(),
                "Bali",
                "10 days in Bali",
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 20),
                1,
                1,
                deadline,
                schedule,
                List.of());
    }
}
