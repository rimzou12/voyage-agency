package com.agencyvoyage.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.agencyvoyage.application.exception.GroupBookingNotFoundException;
import com.agencyvoyage.application.port.in.ConfirmHotelReservationCommand;
import com.agencyvoyage.application.port.in.ConfirmHotelReservationUseCase;
import com.agencyvoyage.application.port.in.CreateGroupBookingUseCase;
import com.agencyvoyage.application.port.in.GetAuditTrailUseCase;
import com.agencyvoyage.application.port.in.GetGroupBookingUseCase;
import com.agencyvoyage.application.port.in.JoinGroupBookingCommand;
import com.agencyvoyage.application.port.in.JoinGroupBookingUseCase;
import com.agencyvoyage.application.port.in.JoinWaitlistCommand;
import com.agencyvoyage.application.port.in.JoinWaitlistUseCase;
import com.agencyvoyage.application.port.in.LeaveGroupBookingCommand;
import com.agencyvoyage.application.port.in.LeaveGroupBookingUseCase;
import com.agencyvoyage.application.port.in.LeaveWaitlistCommand;
import com.agencyvoyage.application.port.in.LeaveWaitlistUseCase;
import com.agencyvoyage.application.port.in.RequestHotelReservationCommand;
import com.agencyvoyage.application.port.in.RequestHotelReservationUseCase;
import com.agencyvoyage.application.port.out.AuditEntry;
import com.agencyvoyage.application.port.out.AuditEventType;
import com.agencyvoyage.domain.booking.GroupBooking;
import com.agencyvoyage.domain.booking.GroupBookingId;
import com.agencyvoyage.domain.booking.GroupBookingStatus;
import com.agencyvoyage.domain.booking.HotelReservationStatus;
import com.agencyvoyage.domain.booking.Participant;
import com.agencyvoyage.domain.booking.ParticipantId;
import com.agencyvoyage.domain.booking.WaitlistEntry;
import com.agencyvoyage.domain.booking.WaitlistEntryId;
import com.agencyvoyage.domain.exception.AlreadyWaitlistedException;
import com.agencyvoyage.domain.exception.BookingNotConfirmedException;
import com.agencyvoyage.domain.exception.BookingNotFullException;
import com.agencyvoyage.domain.exception.GroupFullException;
import com.agencyvoyage.domain.exception.HotelReservationNotPendingException;
import com.agencyvoyage.domain.exception.InvalidReferralException;
import com.agencyvoyage.domain.exception.ParticipantNotInBookingException;
import com.agencyvoyage.domain.trip.PricingSchedule;
import com.agencyvoyage.domain.trip.Trip;
import com.agencyvoyage.domain.trip.TripId;
import com.agencyvoyage.domain.user.User;
import com.agencyvoyage.domain.user.UserId;
import com.agencyvoyage.infrastructure.security.JwtTokenParser;
import com.agencyvoyage.web.config.CorsConfig;
import com.agencyvoyage.web.security.SecurityConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * {@code SecurityConfig} is explicitly imported (it isn't controller-adjacent
 * infrastructure @WebMvcTest would pick up on its own) so {@code @AuthenticationPrincipal}
 * actually resolves: {@code .with(authentication(...))} relies on Spring Security's
 * own context filter to install the test authentication per request, so the filter
 * chain needs to genuinely run here, not be stubbed out.
 */
@WebMvcTest(GroupBookingController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class GroupBookingControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateGroupBookingUseCase createGroupBookingUseCase;

    @MockitoBean
    private JoinGroupBookingUseCase joinGroupBookingUseCase;

    @MockitoBean
    private LeaveGroupBookingUseCase leaveGroupBookingUseCase;

    @MockitoBean
    private GetGroupBookingUseCase getGroupBookingUseCase;

    @MockitoBean
    private GetAuditTrailUseCase getAuditTrailUseCase;

    @MockitoBean
    private JoinWaitlistUseCase joinWaitlistUseCase;

    @MockitoBean
    private LeaveWaitlistUseCase leaveWaitlistUseCase;

    @MockitoBean
    private RequestHotelReservationUseCase requestHotelReservationUseCase;

    @MockitoBean
    private ConfirmHotelReservationUseCase confirmHotelReservationUseCase;

    /**
     * Not used directly by this controller, but JwtAuthenticationFilter is a servlet
     * Filter, so @WebMvcTest's scanning constructs it regardless of which controller
     * is under test - it needs this dependency satisfied to build the context at all.
     */
    @MockitoBean
    private JwtTokenParser jwtTokenParser;

    private static final User ALICE = new User(UserId.newId(), "alice@example.com", "Alice");

    @Test
    void createReturns201WithTheBookingAndMyParticipantId() {
        GroupBooking booking = booking(GroupBookingId.newId(), ALICE.id());
        when(createGroupBookingUseCase.createGroupBooking(any())).thenReturn(booking);

        assertThat(mvc.post()
                        .uri("/api/trips/" + TripId.newId() + "/group-bookings")
                        .with(asAlice()))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.myParticipantId")
                .isNotNull();
    }

    @Test
    void returns409WhenTheDomainRejectsTheJoin() {
        GroupBookingId bookingId = GroupBookingId.newId();
        when(joinGroupBookingUseCase.joinGroupBooking(any(JoinGroupBookingCommand.class)))
                .thenThrow(new GroupFullException(bookingId, 5));

        assertThat(mvc.post()
                        .uri("/api/group-bookings/" + bookingId + "/participants")
                        .with(asAlice()))
                .hasStatus(409);
    }

    @Test
    void joinPassesTheRefQueryParamThroughAsTheReferrer() {
        GroupBookingId bookingId = GroupBookingId.newId();
        ParticipantId referrerId = ParticipantId.newId();
        GroupBooking booking = booking(bookingId, ALICE.id());
        when(joinGroupBookingUseCase.joinGroupBooking(any(JoinGroupBookingCommand.class))).thenReturn(booking);

        assertThat(mvc.post()
                        .uri("/api/group-bookings/" + bookingId + "/participants?ref=" + referrerId)
                        .with(asAlice()))
                .hasStatusOk();

        verify(joinGroupBookingUseCase)
                .joinGroupBooking(new JoinGroupBookingCommand(bookingId, ALICE, referrerId));
    }

    @Test
    void returns409ForAnInvalidReferrer() {
        GroupBookingId bookingId = GroupBookingId.newId();
        when(joinGroupBookingUseCase.joinGroupBooking(any(JoinGroupBookingCommand.class)))
                .thenThrow(new InvalidReferralException(bookingId, ParticipantId.newId()));

        assertThat(mvc.post()
                        .uri("/api/group-bookings/" + bookingId + "/participants?ref=" + ParticipantId.newId())
                        .with(asAlice()))
                .hasStatus(409);
    }

    @Test
    void returns404WhenTheBookingDoesNotExist() {
        GroupBookingId unknownId = GroupBookingId.newId();
        when(getGroupBookingUseCase.getGroupBooking(unknownId))
                .thenThrow(new GroupBookingNotFoundException(unknownId));

        assertThat(mvc.get().uri("/api/group-bookings/" + unknownId)).hasStatus(404);
    }

    @Test
    void getReturnsTheBookingAsJsonWithoutRequiringAuthentication() {
        GroupBookingId bookingId = GroupBookingId.newId();
        GroupBooking booking = booking(bookingId, ALICE.id());
        when(getGroupBookingUseCase.getGroupBooking(bookingId)).thenReturn(booking);

        assertThat(mvc.get().uri("/api/group-bookings/" + bookingId))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("OPEN");
    }

    @Test
    void getReturnsMyPricePerSeatDiscountedWhenIReferredSomeone() {
        GroupBookingId bookingId = GroupBookingId.newId();
        GroupBooking booking = bookingWithAReferral(bookingId, ALICE.id());
        when(getGroupBookingUseCase.getGroupBooking(bookingId)).thenReturn(booking);

        assertThat(mvc.get().uri("/api/group-bookings/" + bookingId).with(asAlice()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.myPricePerSeat")
                .isEqualTo(950.0);
    }

    @Test
    void leaveReturnsTheUpdatedBookingAsJson() {
        GroupBookingId bookingId = GroupBookingId.newId();
        GroupBooking booking = booking(bookingId, ALICE.id());
        when(leaveGroupBookingUseCase.leaveGroupBooking(any(LeaveGroupBookingCommand.class)))
                .thenReturn(booking);

        assertThat(mvc.delete()
                        .uri("/api/group-bookings/" + bookingId + "/participants/me")
                        .with(asAlice()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("OPEN");
    }

    @Test
    void returns409WhenLeavingAndYouAreNotAParticipant() {
        GroupBookingId bookingId = GroupBookingId.newId();
        when(leaveGroupBookingUseCase.leaveGroupBooking(any(LeaveGroupBookingCommand.class)))
                .thenThrow(new ParticipantNotInBookingException(bookingId, ALICE.id()));

        assertThat(mvc.delete()
                        .uri("/api/group-bookings/" + bookingId + "/participants/me")
                        .with(asAlice()))
                .hasStatus(409);
    }

    @Test
    void getAuditTrailReturnsTheStoredHistoryAsJsonWithoutRequiringAuthentication() {
        GroupBookingId bookingId = GroupBookingId.newId();
        AuditEntry entry = new AuditEntry(
                bookingId, AuditEventType.PARTICIPANT_JOINED, ParticipantId.newId(), "Alice", 1, new BigDecimal("1000"), null, Instant.now());
        when(getAuditTrailUseCase.getAuditTrail(bookingId)).thenReturn(List.of(entry));

        assertThat(mvc.get().uri("/api/group-bookings/" + bookingId + "/audit-trail"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].type")
                .isEqualTo("PARTICIPANT_JOINED");
    }

    @Test
    void joinWaitlistReturns200WithTheBookingAndMyWaitlistEntryId() {
        GroupBookingId bookingId = GroupBookingId.newId();
        GroupBooking booking = fullBookingWithOneWaitlisted(bookingId, ALICE.id());
        when(joinWaitlistUseCase.joinWaitlist(any(JoinWaitlistCommand.class))).thenReturn(booking);

        assertThat(mvc.post()
                        .uri("/api/group-bookings/" + bookingId + "/waitlist")
                        .with(asAlice()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.myWaitlistEntryId")
                .isNotNull();
    }

    @Test
    void returns409WhenJoiningTheWaitlistOfABookingThatIsNotFull() {
        GroupBookingId bookingId = GroupBookingId.newId();
        when(joinWaitlistUseCase.joinWaitlist(any(JoinWaitlistCommand.class)))
                .thenThrow(new BookingNotFullException(bookingId));

        assertThat(mvc.post()
                        .uri("/api/group-bookings/" + bookingId + "/waitlist")
                        .with(asAlice()))
                .hasStatus(409);
    }

    @Test
    void returns409WhenAlreadyOnTheWaitlist() {
        GroupBookingId bookingId = GroupBookingId.newId();
        when(joinWaitlistUseCase.joinWaitlist(any(JoinWaitlistCommand.class)))
                .thenThrow(new AlreadyWaitlistedException(bookingId, ALICE.id()));

        assertThat(mvc.post()
                        .uri("/api/group-bookings/" + bookingId + "/waitlist")
                        .with(asAlice()))
                .hasStatus(409);
    }

    @Test
    void leaveWaitlistReturnsTheUpdatedBookingAsJson() {
        GroupBookingId bookingId = GroupBookingId.newId();
        GroupBooking booking = booking(bookingId, ALICE.id());
        when(leaveWaitlistUseCase.leaveWaitlist(any(LeaveWaitlistCommand.class))).thenReturn(booking);

        assertThat(mvc.delete()
                        .uri("/api/group-bookings/" + bookingId + "/waitlist/me")
                        .with(asAlice()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("OPEN");
    }

    @Test
    void requestHotelReservationReturnsTheUpdatedBookingAsJson() {
        GroupBookingId bookingId = GroupBookingId.newId();
        GroupBooking booking = booking(bookingId, ALICE.id());
        when(requestHotelReservationUseCase.requestHotelReservation(any(RequestHotelReservationCommand.class)))
                .thenReturn(booking);

        assertThat(mvc.post()
                        .uri("/api/group-bookings/" + bookingId + "/hotel-reservation")
                        .with(asAlice())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"reference\":\"REF-1\"}"))
                .hasStatusOk();

        verify(requestHotelReservationUseCase)
                .requestHotelReservation(new RequestHotelReservationCommand(bookingId, "REF-1"));
    }

    @Test
    void returns409WhenRequestingAHotelReservationForABookingThatIsNotConfirmed() {
        GroupBookingId bookingId = GroupBookingId.newId();
        when(requestHotelReservationUseCase.requestHotelReservation(any(RequestHotelReservationCommand.class)))
                .thenThrow(new BookingNotConfirmedException(bookingId, GroupBookingStatus.OPEN));

        assertThat(mvc.post()
                        .uri("/api/group-bookings/" + bookingId + "/hotel-reservation")
                        .with(asAlice())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"reference\":\"REF-1\"}"))
                .hasStatus(409);
    }

    @Test
    void confirmHotelReservationReturnsTheUpdatedBookingAsJson() {
        GroupBookingId bookingId = GroupBookingId.newId();
        GroupBooking booking = booking(bookingId, ALICE.id());
        when(confirmHotelReservationUseCase.confirmHotelReservation(any(ConfirmHotelReservationCommand.class)))
                .thenReturn(booking);

        assertThat(mvc.post()
                        .uri("/api/group-bookings/" + bookingId + "/hotel-reservation/confirm")
                        .with(asAlice()))
                .hasStatusOk();

        verify(confirmHotelReservationUseCase)
                .confirmHotelReservation(new ConfirmHotelReservationCommand(bookingId));
    }

    @Test
    void returns409WhenConfirmingAHotelReservationThatIsNotPending() {
        GroupBookingId bookingId = GroupBookingId.newId();
        when(confirmHotelReservationUseCase.confirmHotelReservation(any(ConfirmHotelReservationCommand.class)))
                .thenThrow(new HotelReservationNotPendingException(bookingId, HotelReservationStatus.NOT_REQUESTED));

        assertThat(mvc.post()
                        .uri("/api/group-bookings/" + bookingId + "/hotel-reservation/confirm")
                        .with(asAlice()))
                .hasStatus(409);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asAlice() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(ALICE, null, List.of());
        return authentication(authentication);
    }

    private static GroupBooking booking(GroupBookingId id, UserId creatorUserId) {
        PricingSchedule schedule = PricingSchedule.of(new BigDecimal("1000"), List.of(), 5);
        Trip trip = new Trip(
                TripId.newId(),
                "Bali",
                "desc",
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 20),
                2,
                5,
                Instant.now().plus(30, ChronoUnit.DAYS),
                schedule,
                List.of());
        Participant creator = new Participant(ParticipantId.newId(), creatorUserId, "Alice", Instant.now());
        return GroupBooking.reconstitute(
                id,
                trip.id(),
                trip.minParticipants(),
                trip.maxParticipants(),
                trip.bookingDeadline(),
                schedule,
                GroupBookingStatus.OPEN,
                List.of(creator),
                List.of(),
                HotelReservationStatus.NOT_REQUESTED,
                null);
    }

    private static GroupBooking bookingWithAReferral(GroupBookingId id, UserId referrerUserId) {
        PricingSchedule schedule = PricingSchedule.of(new BigDecimal("1000"), List.of(), 5);
        Trip trip = new Trip(
                TripId.newId(),
                "Bali",
                "desc",
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 20),
                2,
                5,
                Instant.now().plus(30, ChronoUnit.DAYS),
                schedule,
                List.of());
        Participant referrer = new Participant(ParticipantId.newId(), referrerUserId, "Alice", Instant.now());
        Participant referred = new Participant(
                ParticipantId.newId(), UserId.newId(), "Bob", Instant.now(), referrer.id());
        return GroupBooking.reconstitute(
                id,
                trip.id(),
                trip.minParticipants(),
                trip.maxParticipants(),
                trip.bookingDeadline(),
                schedule,
                GroupBookingStatus.OPEN,
                List.of(referrer, referred),
                List.of(),
                HotelReservationStatus.NOT_REQUESTED,
                null);
    }

    private static GroupBooking fullBookingWithOneWaitlisted(GroupBookingId id, UserId waitlistedUserId) {
        PricingSchedule schedule = PricingSchedule.of(new BigDecimal("1000"), List.of(), 1);
        Trip trip = new Trip(
                TripId.newId(),
                "Bali",
                "desc",
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 20),
                1,
                1,
                Instant.now().plus(30, ChronoUnit.DAYS),
                schedule,
                List.of());
        Participant creator = new Participant(ParticipantId.newId(), UserId.newId(), "Bob", Instant.now());
        WaitlistEntry entry =
                new WaitlistEntry(WaitlistEntryId.newId(), waitlistedUserId, "Alice", Instant.now());
        return GroupBooking.reconstitute(
                id,
                trip.id(),
                trip.minParticipants(),
                trip.maxParticipants(),
                trip.bookingDeadline(),
                schedule,
                GroupBookingStatus.OPEN,
                List.of(creator),
                List.of(entry),
                HotelReservationStatus.NOT_REQUESTED,
                null);
    }
}
