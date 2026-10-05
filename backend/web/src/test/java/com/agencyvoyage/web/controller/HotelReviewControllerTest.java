package com.agencyvoyage.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.agencyvoyage.application.exception.AlreadyReviewedHotelException;
import com.agencyvoyage.application.exception.HotelReviewNotFoundException;
import com.agencyvoyage.application.exception.NotReviewAuthorException;
import com.agencyvoyage.application.port.in.AddHotelReviewCommand;
import com.agencyvoyage.application.port.in.AddHotelReviewUseCase;
import com.agencyvoyage.application.port.in.DeleteHotelReviewCommand;
import com.agencyvoyage.application.port.in.DeleteHotelReviewUseCase;
import com.agencyvoyage.application.port.in.ListHotelReviewsUseCase;
import com.agencyvoyage.application.port.in.UpdateHotelReviewCommand;
import com.agencyvoyage.application.port.in.UpdateHotelReviewUseCase;
import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.hotel.HotelReview;
import com.agencyvoyage.domain.hotel.HotelReviewId;
import com.agencyvoyage.domain.user.User;
import com.agencyvoyage.domain.user.UserId;
import com.agencyvoyage.infrastructure.security.JwtTokenParser;
import com.agencyvoyage.web.config.CorsConfig;
import com.agencyvoyage.web.security.SecurityConfig;
import java.time.Instant;
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

@WebMvcTest(HotelReviewController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class HotelReviewControllerTest {

    private static final User ALICE = new User(UserId.newId(), "alice@example.com", "Alice");
    private static final User BOB = new User(UserId.newId(), "bob@example.com", "Bob");
    private static final User ADMIN = new User(UserId.newId(), "admin@example.com", "Admin", true);

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AddHotelReviewUseCase addHotelReviewUseCase;

    @MockitoBean
    private ListHotelReviewsUseCase listHotelReviewsUseCase;

    @MockitoBean
    private UpdateHotelReviewUseCase updateHotelReviewUseCase;

    @MockitoBean
    private DeleteHotelReviewUseCase deleteHotelReviewUseCase;

    /**
     * Not used by HotelReviewController, but JwtAuthenticationFilter is a servlet
     * Filter, so @WebMvcTest's scanning constructs it regardless of which controller
     * is under test - it needs this dependency satisfied to build the context at all.
     */
    @MockitoBean
    private JwtTokenParser jwtTokenParser;

    @Test
    void aLoggedInUserCanAddAReviewReturning201() {
        HotelId hotelId = HotelId.newId();
        HotelReview review = new HotelReview(
                HotelReviewId.newId(), hotelId, ALICE.id(), "Alice", 5, "Loved it!", Instant.now());
        when(addHotelReviewUseCase.addReview(any(AddHotelReviewCommand.class))).thenReturn(review);

        assertThat(mvc.post()
                        .uri("/api/hotels/" + hotelId + "/reviews")
                        .with(asAlice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Loved it!\"}"))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.rating")
                .isEqualTo(5);
    }

    @Test
    void returns401WhenAddingAReviewWithoutAuthentication() {
        HotelId hotelId = HotelId.newId();

        assertThat(mvc.post()
                        .uri("/api/hotels/" + hotelId + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":null}"))
                .hasStatus(401);
    }

    @Test
    void returns409WhenTheUserAlreadyReviewedThisHotel() {
        HotelId hotelId = HotelId.newId();
        when(addHotelReviewUseCase.addReview(any(AddHotelReviewCommand.class)))
                .thenThrow(new AlreadyReviewedHotelException());

        assertThat(mvc.post()
                        .uri("/api/hotels/" + hotelId + "/reviews")
                        .with(asAlice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":null}"))
                .hasStatus(409);
    }

    @Test
    void listReviewsReturnsThemAsJsonWithoutRequiringAuthentication() {
        HotelId hotelId = HotelId.newId();
        HotelReview review =
                new HotelReview(HotelReviewId.newId(), hotelId, ALICE.id(), "Alice", 4, "Nice", Instant.now());
        when(listHotelReviewsUseCase.listReviews(hotelId)).thenReturn(List.of(review));

        assertThat(mvc.get().uri("/api/hotels/" + hotelId + "/reviews"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].authorName")
                .isEqualTo("Alice");
    }

    @Test
    void theAuthorCanUpdateTheirOwnReview() {
        HotelId hotelId = HotelId.newId();
        HotelReviewId reviewId = HotelReviewId.newId();
        HotelReview updated = new HotelReview(
                reviewId, hotelId, ALICE.id(), "Alice", 3, "Changed my mind", Instant.now());
        when(updateHotelReviewUseCase.updateReview(any(UpdateHotelReviewCommand.class))).thenReturn(updated);

        assertThat(mvc.put()
                        .uri("/api/hotels/" + hotelId + "/reviews/" + reviewId)
                        .with(asAlice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":3,\"comment\":\"Changed my mind\"}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.rating")
                .isEqualTo(3);
    }

    @Test
    void returns403WhenUpdatingSomeoneElsesReview() {
        HotelId hotelId = HotelId.newId();
        HotelReviewId reviewId = HotelReviewId.newId();
        when(updateHotelReviewUseCase.updateReview(any(UpdateHotelReviewCommand.class)))
                .thenThrow(new NotReviewAuthorException());

        assertThat(mvc.put()
                        .uri("/api/hotels/" + hotelId + "/reviews/" + reviewId)
                        .with(asBob())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":1,\"comment\":null}"))
                .hasStatus(403);
    }

    @Test
    void returns404WhenUpdatingAReviewThatDoesNotExist() {
        HotelId hotelId = HotelId.newId();
        HotelReviewId unknownId = HotelReviewId.newId();
        when(updateHotelReviewUseCase.updateReview(any(UpdateHotelReviewCommand.class)))
                .thenThrow(new HotelReviewNotFoundException(unknownId));

        assertThat(mvc.put()
                        .uri("/api/hotels/" + hotelId + "/reviews/" + unknownId)
                        .with(asAlice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":3,\"comment\":null}"))
                .hasStatus(404);
    }

    @Test
    void theAuthorCanDeleteTheirOwnReview() {
        HotelId hotelId = HotelId.newId();
        HotelReviewId reviewId = HotelReviewId.newId();
        doNothing().when(deleteHotelReviewUseCase).deleteReview(any(DeleteHotelReviewCommand.class));

        assertThat(mvc.delete().uri("/api/hotels/" + hotelId + "/reviews/" + reviewId).with(asAlice()))
                .hasStatus(204);
        verify(deleteHotelReviewUseCase).deleteReview(new DeleteHotelReviewCommand(reviewId, ALICE));
    }

    @Test
    void anAdminCanDeleteAnyonesReview() {
        HotelId hotelId = HotelId.newId();
        HotelReviewId reviewId = HotelReviewId.newId();
        doNothing().when(deleteHotelReviewUseCase).deleteReview(any(DeleteHotelReviewCommand.class));

        assertThat(mvc.delete().uri("/api/hotels/" + hotelId + "/reviews/" + reviewId).with(asAdmin()))
                .hasStatus(204);
    }

    @Test
    void returns403WhenDeletingSomeoneElsesReviewAsARegularUser() {
        HotelId hotelId = HotelId.newId();
        HotelReviewId reviewId = HotelReviewId.newId();
        doThrow(new NotReviewAuthorException())
                .when(deleteHotelReviewUseCase)
                .deleteReview(any(DeleteHotelReviewCommand.class));

        assertThat(mvc.delete().uri("/api/hotels/" + hotelId + "/reviews/" + reviewId).with(asBob()))
                .hasStatus(403);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asAlice() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(ALICE, null, List.of());
        return authentication(authentication);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asBob() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(BOB, null, List.of());
        return authentication(authentication);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asAdmin() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(ADMIN, null, List.of());
        return authentication(authentication);
    }
}
