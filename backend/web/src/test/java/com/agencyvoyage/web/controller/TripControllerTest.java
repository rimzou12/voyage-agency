package com.agencyvoyage.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.agencyvoyage.application.exception.TripNotFoundException;
import com.agencyvoyage.application.port.in.CreateTripCommand;
import com.agencyvoyage.application.port.in.CreateTripUseCase;
import com.agencyvoyage.application.port.in.DeleteTripCommand;
import com.agencyvoyage.application.port.in.DeleteTripUseCase;
import com.agencyvoyage.application.port.in.GetTripUseCase;
import com.agencyvoyage.application.port.in.ListTripsUseCase;
import com.agencyvoyage.application.port.in.UpdateTripCommand;
import com.agencyvoyage.application.port.in.UpdateTripUseCase;
import com.agencyvoyage.application.exception.NotAnAdminException;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(TripController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class TripControllerTest {

    private static final User ADMIN = new User(UserId.newId(), "admin@example.com", "Admin", true);
    private static final User ALICE = new User(UserId.newId(), "alice@example.com", "Alice");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListTripsUseCase listTripsUseCase;

    @MockitoBean
    private GetTripUseCase getTripUseCase;

    @MockitoBean
    private CreateTripUseCase createTripUseCase;

    @MockitoBean
    private UpdateTripUseCase updateTripUseCase;

    @MockitoBean
    private DeleteTripUseCase deleteTripUseCase;

    /**
     * Not used by TripController, but JwtAuthenticationFilter is a servlet Filter, so
     * @WebMvcTest's scanning constructs it regardless of which controller is under
     * test - it needs this dependency satisfied to build the context at all.
     */
    @MockitoBean
    private JwtTokenParser jwtTokenParser;

    @Test
    void listsTripsAsJson() {
        when(listTripsUseCase.listTrips()).thenReturn(List.of(trip()));

        assertThat(mvc.get().uri("/api/trips"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].destination")
                .isEqualTo("Bali");
    }

    @Test
    void returns404WhenTheTripDoesNotExist() {
        TripId unknownId = TripId.newId();
        when(getTripUseCase.getTrip(unknownId)).thenThrow(new TripNotFoundException(unknownId));

        assertThat(mvc.get().uri("/api/trips/" + unknownId)).hasStatus(404);
    }

    @Test
    void anAdminCanCreateATripReturning201() {
        when(createTripUseCase.createTrip(any(CreateTripCommand.class))).thenReturn(trip());

        assertThat(mvc.post()
                        .uri("/api/trips")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"destination\":\"Bali\",\"description\":\"desc\",\"departureDate\":\"2027-06-10\","
                                        + "\"returnDate\":\"2027-06-20\",\"minParticipants\":2,\"maxParticipants\":5,"
                                        + "\"bookingDeadline\":\"2027-05-01T00:00:00Z\",\"basePrice\":1000,\"priceTiers\":[]}"))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.destination")
                .isEqualTo("Bali");
    }

    @Test
    void returns403WhenANonAdminTriesToCreateATrip() {
        when(createTripUseCase.createTrip(any(CreateTripCommand.class))).thenThrow(new NotAnAdminException());

        assertThat(mvc.post()
                        .uri("/api/trips")
                        .with(asAlice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"destination\":\"Bali\",\"description\":\"desc\",\"departureDate\":\"2027-06-10\","
                                        + "\"returnDate\":\"2027-06-20\",\"minParticipants\":2,\"maxParticipants\":5,"
                                        + "\"bookingDeadline\":\"2027-05-01T00:00:00Z\",\"basePrice\":1000,\"priceTiers\":[]}"))
                .hasStatus(403);
    }

    @Test
    void anAdminCanUpdateATrip() {
        Trip trip = trip();
        when(updateTripUseCase.updateTrip(any(UpdateTripCommand.class))).thenReturn(trip);

        assertThat(mvc.put()
                        .uri("/api/trips/" + trip.id())
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"destination\":\"Bali\",\"description\":\"desc\",\"departureDate\":\"2027-06-10\","
                                        + "\"returnDate\":\"2027-06-20\",\"minParticipants\":2,\"maxParticipants\":5,"
                                        + "\"bookingDeadline\":\"2027-05-01T00:00:00Z\",\"basePrice\":1000,\"priceTiers\":[]}"))
                .hasStatusOk();
    }

    @Test
    void returns404WhenUpdatingATripThatDoesNotExist() {
        TripId unknownId = TripId.newId();
        when(updateTripUseCase.updateTrip(any(UpdateTripCommand.class)))
                .thenThrow(new TripNotFoundException(unknownId));

        assertThat(mvc.put()
                        .uri("/api/trips/" + unknownId)
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"destination\":\"Bali\",\"description\":\"desc\",\"departureDate\":\"2027-06-10\","
                                        + "\"returnDate\":\"2027-06-20\",\"minParticipants\":2,\"maxParticipants\":5,"
                                        + "\"bookingDeadline\":\"2027-05-01T00:00:00Z\",\"basePrice\":1000,\"priceTiers\":[]}"))
                .hasStatus(404);
    }

    @Test
    void anAdminCanDeleteATrip() {
        TripId tripId = TripId.newId();
        doNothing().when(deleteTripUseCase).deleteTrip(any(DeleteTripCommand.class));

        assertThat(mvc.delete().uri("/api/trips/" + tripId).with(asAdmin())).hasStatus(204);
        verify(deleteTripUseCase).deleteTrip(new DeleteTripCommand(tripId, ADMIN));
    }

    @Test
    void returns403WhenANonAdminTriesToDeleteATrip() {
        TripId tripId = TripId.newId();
        doThrow(new NotAnAdminException()).when(deleteTripUseCase).deleteTrip(any(DeleteTripCommand.class));

        assertThat(mvc.delete().uri("/api/trips/" + tripId).with(asAlice())).hasStatus(403);
    }

    @Test
    void returns404WhenDeletingATripThatDoesNotExist() {
        TripId unknownId = TripId.newId();
        doThrow(new TripNotFoundException(unknownId)).when(deleteTripUseCase).deleteTrip(any(DeleteTripCommand.class));

        assertThat(mvc.delete().uri("/api/trips/" + unknownId).with(asAdmin())).hasStatus(404);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asAdmin() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(ADMIN, null, List.of());
        return authentication(authentication);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asAlice() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(ALICE, null, List.of());
        return authentication(authentication);
    }

    private static Trip trip() {
        PricingSchedule schedule = PricingSchedule.of(new BigDecimal("1000"), List.of(), 5);
        return new Trip(
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
    }
}
