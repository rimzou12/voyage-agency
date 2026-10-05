package com.agencyvoyage.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.agencyvoyage.application.exception.HotelNotFoundException;
import com.agencyvoyage.application.exception.NotAnAdminException;
import com.agencyvoyage.application.port.in.AddHotelCommand;
import com.agencyvoyage.application.port.in.AddHotelUseCase;
import com.agencyvoyage.application.port.in.DeleteHotelCommand;
import com.agencyvoyage.application.port.in.DeleteHotelUseCase;
import com.agencyvoyage.application.port.in.ListHotelsForTripUseCase;
import com.agencyvoyage.application.port.in.UpdateHotelCommand;
import com.agencyvoyage.application.port.in.UpdateHotelUseCase;
import com.agencyvoyage.domain.hotel.Hotel;
import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.trip.TripId;
import com.agencyvoyage.domain.user.User;
import com.agencyvoyage.domain.user.UserId;
import com.agencyvoyage.infrastructure.security.JwtTokenParser;
import com.agencyvoyage.web.config.CorsConfig;
import com.agencyvoyage.web.security.SecurityConfig;
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

@WebMvcTest(HotelController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class HotelControllerTest {

    private static final User ADMIN = new User(UserId.newId(), "admin@example.com", "Admin", true);
    private static final User ALICE = new User(UserId.newId(), "alice@example.com", "Alice");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AddHotelUseCase addHotelUseCase;

    @MockitoBean
    private ListHotelsForTripUseCase listHotelsForTripUseCase;

    @MockitoBean
    private UpdateHotelUseCase updateHotelUseCase;

    @MockitoBean
    private DeleteHotelUseCase deleteHotelUseCase;

    /**
     * Not used by HotelController, but JwtAuthenticationFilter is a servlet Filter, so
     * @WebMvcTest's scanning constructs it regardless of which controller is under
     * test - it needs this dependency satisfied to build the context at all.
     */
    @MockitoBean
    private JwtTokenParser jwtTokenParser;

    @Test
    void anAdminCanAddAHotelReturning201() {
        TripId tripId = TripId.newId();
        Hotel hotel = new Hotel(
                HotelId.newId(),
                tripId,
                "Ubud Retreat",
                "Jungle views",
                List.of("https://x/a.jpg"),
                List.of("Restaurant", "Pool"));
        when(addHotelUseCase.addHotel(any(AddHotelCommand.class))).thenReturn(hotel);

        assertThat(mvc.post()
                        .uri("/api/trips/" + tripId + "/hotels")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"name\":\"Ubud Retreat\",\"description\":\"Jungle views\",\"photoUrls\":[\"https://x/a.jpg\"],\"amenities\":[\"Restaurant\",\"Pool\"]}"))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.amenities[0]")
                .isEqualTo("Restaurant");
    }

    @Test
    void returns403WhenANonAdminTriesToAddAHotel() {
        TripId tripId = TripId.newId();
        when(addHotelUseCase.addHotel(any(AddHotelCommand.class))).thenThrow(new NotAnAdminException());

        assertThat(mvc.post()
                        .uri("/api/trips/" + tripId + "/hotels")
                        .with(asAlice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ubud Retreat\",\"description\":\"Jungle views\",\"photoUrls\":[]}"))
                .hasStatus(403);
    }

    @Test
    void returns401WhenAddingAHotelWithoutAuthentication() {
        TripId tripId = TripId.newId();

        assertThat(mvc.post()
                        .uri("/api/trips/" + tripId + "/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ubud Retreat\",\"description\":\"Jungle views\",\"photoUrls\":[]}"))
                .hasStatus(401);
    }

    @Test
    void listHotelsReturnsThemAsJsonWithoutRequiringAuthentication() {
        TripId tripId = TripId.newId();
        Hotel hotel = new Hotel(HotelId.newId(), tripId, "Ubud Retreat", "Jungle views", List.of(), List.of());
        when(listHotelsForTripUseCase.listHotels(tripId)).thenReturn(List.of(hotel));

        assertThat(mvc.get().uri("/api/trips/" + tripId + "/hotels"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].name")
                .isEqualTo("Ubud Retreat");
    }

    @Test
    void anAdminCanUpdateAHotel() {
        TripId tripId = TripId.newId();
        HotelId hotelId = HotelId.newId();
        Hotel updated = new Hotel(hotelId, tripId, "Renamed Retreat", "Updated views", List.of(), List.of());
        when(updateHotelUseCase.updateHotel(any(UpdateHotelCommand.class))).thenReturn(updated);

        assertThat(mvc.put()
                        .uri("/api/trips/" + tripId + "/hotels/" + hotelId)
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed Retreat\",\"description\":\"Updated views\",\"photoUrls\":[]}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("Renamed Retreat");
    }

    @Test
    void returns404WhenUpdatingAHotelThatDoesNotExist() {
        TripId tripId = TripId.newId();
        HotelId unknownId = HotelId.newId();
        when(updateHotelUseCase.updateHotel(any(UpdateHotelCommand.class)))
                .thenThrow(new HotelNotFoundException(unknownId));

        assertThat(mvc.put()
                        .uri("/api/trips/" + tripId + "/hotels/" + unknownId)
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\",\"description\":\"Updated\",\"photoUrls\":[]}"))
                .hasStatus(404);
    }

    @Test
    void anAdminCanDeleteAHotel() {
        TripId tripId = TripId.newId();
        HotelId hotelId = HotelId.newId();
        doNothing().when(deleteHotelUseCase).deleteHotel(any(DeleteHotelCommand.class));

        assertThat(mvc.delete().uri("/api/trips/" + tripId + "/hotels/" + hotelId).with(asAdmin()))
                .hasStatus(204);
        verify(deleteHotelUseCase).deleteHotel(new DeleteHotelCommand(hotelId, ADMIN));
    }

    @Test
    void returns403WhenANonAdminTriesToDeleteAHotel() {
        TripId tripId = TripId.newId();
        HotelId hotelId = HotelId.newId();
        doThrow(new NotAnAdminException()).when(deleteHotelUseCase).deleteHotel(any(DeleteHotelCommand.class));

        assertThat(mvc.delete().uri("/api/trips/" + tripId + "/hotels/" + hotelId).with(asAlice()))
                .hasStatus(403);
    }

    @Test
    void returns404WhenDeletingAHotelThatDoesNotExist() {
        TripId tripId = TripId.newId();
        HotelId unknownId = HotelId.newId();
        doThrow(new HotelNotFoundException(unknownId))
                .when(deleteHotelUseCase)
                .deleteHotel(any(DeleteHotelCommand.class));

        assertThat(mvc.delete().uri("/api/trips/" + tripId + "/hotels/" + unknownId).with(asAdmin()))
                .hasStatus(404);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asAdmin() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(ADMIN, null, List.of());
        return authentication(authentication);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asAlice() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(ALICE, null, List.of());
        return authentication(authentication);
    }
}
