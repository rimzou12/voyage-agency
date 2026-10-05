package com.agencyvoyage.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.agencyvoyage.web.dto.AuthResponse;
import com.agencyvoyage.web.dto.HotelResponse;
import com.agencyvoyage.web.dto.HotelReviewResponse;
import com.agencyvoyage.web.dto.LoginRequest;
import com.agencyvoyage.web.dto.RegisterRequest;
import com.agencyvoyage.web.dto.TripResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Verifies the per-hotel review flow end to end: any logged-in user can rate and
 * review a hotel at most once, edit or delete their own review, and an admin can
 * delete anyone's.
 */
class HotelReviewApiIT extends AbstractApiIT {

    private final RestTemplate rest = new RestTemplate();

    @Test
    void aLoggedInUserCanReviewAHotelAndAnyoneCanReadItBack() {
        String adminToken = loginAsSeededAdmin();
        String hotelId = addHotelToFirstTrip(adminToken);
        String aliceToken = registerAndLogin("Alice");

        HotelReviewResponse created = rest.exchange(
                        baseUrl() + "/api/hotels/" + hotelId + "/reviews",
                        HttpMethod.POST,
                        authed(aliceToken, "{\"rating\":5,\"comment\":\"Loved it!\"}"),
                        HotelReviewResponse.class)
                .getBody();

        assertThat(created.rating()).isEqualTo(5);
        assertThat(created.comment()).isEqualTo("Loved it!");
        assertThat(created.authorName()).isEqualTo("Alice");

        HotelReviewResponse[] reviews =
                rest.getForObject(baseUrl() + "/api/hotels/" + hotelId + "/reviews", HotelReviewResponse[].class);
        assertThat(reviews).extracting(HotelReviewResponse::authorName).contains("Alice");
    }

    @Test
    void requiresAuthenticationToAddAReview() {
        String adminToken = loginAsSeededAdmin();
        String hotelId = addHotelToFirstTrip(adminToken);

        try {
            rest.postForObject(
                    baseUrl() + "/api/hotels/" + hotelId + "/reviews",
                    "{\"rating\":5,\"comment\":null}",
                    String.class);
            org.junit.jupiter.api.Assertions.fail("expected a 401");
        } catch (RestClientException e) {
            assertThat(e.getMessage()).contains("401");
        }
    }

    @Test
    void aUserCannotReviewTheSameHotelTwice() {
        String adminToken = loginAsSeededAdmin();
        String hotelId = addHotelToFirstTrip(adminToken);
        String aliceToken = registerAndLogin("Alice");
        rest.exchange(
                baseUrl() + "/api/hotels/" + hotelId + "/reviews",
                HttpMethod.POST,
                authed(aliceToken, "{\"rating\":5,\"comment\":null}"),
                HotelReviewResponse.class);

        try {
            rest.exchange(
                    baseUrl() + "/api/hotels/" + hotelId + "/reviews",
                    HttpMethod.POST,
                    authed(aliceToken, "{\"rating\":3,\"comment\":null}"),
                    HotelReviewResponse.class);
            org.junit.jupiter.api.Assertions.fail("expected a 409, Alice already reviewed this hotel");
        } catch (RestClientException e) {
            assertThat(e.getMessage()).contains("409");
        }
    }

    @Test
    void theAuthorCanEditAndDeleteTheirOwnReview() {
        String adminToken = loginAsSeededAdmin();
        String hotelId = addHotelToFirstTrip(adminToken);
        String aliceToken = registerAndLogin("Alice");
        HotelReviewResponse created = rest.exchange(
                        baseUrl() + "/api/hotels/" + hotelId + "/reviews",
                        HttpMethod.POST,
                        authed(aliceToken, "{\"rating\":3,\"comment\":\"It was ok\"}"),
                        HotelReviewResponse.class)
                .getBody();

        HotelReviewResponse updated = rest.exchange(
                        baseUrl() + "/api/hotels/" + hotelId + "/reviews/" + created.id(),
                        HttpMethod.PUT,
                        authed(aliceToken, "{\"rating\":5,\"comment\":\"Actually loved it!\"}"),
                        HotelReviewResponse.class)
                .getBody();
        assertThat(updated.rating()).isEqualTo(5);

        rest.exchange(
                baseUrl() + "/api/hotels/" + hotelId + "/reviews/" + created.id(),
                HttpMethod.DELETE,
                authed(aliceToken, null),
                Void.class);

        List<HotelReviewResponse> remaining = List.of(rest.getForObject(
                baseUrl() + "/api/hotels/" + hotelId + "/reviews", HotelReviewResponse[].class));
        assertThat(remaining).isEmpty();
    }

    @Test
    void aRegularUserCannotEditOrDeleteSomeoneElsesReview() {
        String adminToken = loginAsSeededAdmin();
        String hotelId = addHotelToFirstTrip(adminToken);
        String aliceToken = registerAndLogin("Alice");
        String bobToken = registerAndLogin("Bob");
        HotelReviewResponse created = rest.exchange(
                        baseUrl() + "/api/hotels/" + hotelId + "/reviews",
                        HttpMethod.POST,
                        authed(aliceToken, "{\"rating\":3,\"comment\":null}"),
                        HotelReviewResponse.class)
                .getBody();

        try {
            rest.exchange(
                    baseUrl() + "/api/hotels/" + hotelId + "/reviews/" + created.id(),
                    HttpMethod.PUT,
                    authed(bobToken, "{\"rating\":1,\"comment\":null}"),
                    HotelReviewResponse.class);
            org.junit.jupiter.api.Assertions.fail("expected a 403, Bob didn't write this review");
        } catch (RestClientException e) {
            assertThat(e.getMessage()).contains("403");
        }

        try {
            rest.exchange(
                    baseUrl() + "/api/hotels/" + hotelId + "/reviews/" + created.id(),
                    HttpMethod.DELETE,
                    authed(bobToken, null),
                    Void.class);
            org.junit.jupiter.api.Assertions.fail("expected a 403, Bob didn't write this review");
        } catch (RestClientException e) {
            assertThat(e.getMessage()).contains("403");
        }
    }

    @Test
    void anAdminCanDeleteAnyonesReview() {
        String adminToken = loginAsSeededAdmin();
        String hotelId = addHotelToFirstTrip(adminToken);
        String aliceToken = registerAndLogin("Alice");
        HotelReviewResponse created = rest.exchange(
                        baseUrl() + "/api/hotels/" + hotelId + "/reviews",
                        HttpMethod.POST,
                        authed(aliceToken, "{\"rating\":3,\"comment\":null}"),
                        HotelReviewResponse.class)
                .getBody();

        rest.exchange(
                baseUrl() + "/api/hotels/" + hotelId + "/reviews/" + created.id(),
                HttpMethod.DELETE,
                authed(adminToken, null),
                Void.class);

        List<HotelReviewResponse> remaining = List.of(rest.getForObject(
                baseUrl() + "/api/hotels/" + hotelId + "/reviews", HotelReviewResponse[].class));
        assertThat(remaining).isEmpty();
    }

    private String addHotelToFirstTrip(String adminToken) {
        TripResponse trip = rest.getForObject(baseUrl() + "/api/trips", TripResponse[].class)[0];
        HotelResponse hotel = rest.exchange(
                        baseUrl() + "/api/trips/" + trip.id() + "/hotels",
                        HttpMethod.POST,
                        authed(
                                adminToken,
                                "{\"name\":\"Ubud Retreat\",\"description\":\"Jungle views\",\"photoUrls\":[],\"amenities\":[]}"),
                        HotelResponse.class)
                .getBody();
        return hotel.id();
    }

    private String loginAsSeededAdmin() {
        AuthResponse response = rest.postForObject(
                baseUrl() + "/api/auth/login",
                new LoginRequest("admin@agencyvoyage.example", "admin12345"),
                AuthResponse.class);
        return response.token();
    }

    private String registerAndLogin(String displayName) {
        String email = displayName.toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        AuthResponse response = rest.postForObject(
                baseUrl() + "/api/auth/register", new RegisterRequest(email, "password123", displayName), AuthResponse.class);
        return response.token();
    }

    private <T> HttpEntity<T> authed(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
