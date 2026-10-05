package com.agencyvoyage.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.agencyvoyage.web.dto.AuthResponse;
import com.agencyvoyage.web.dto.HotelResponse;
import com.agencyvoyage.web.dto.LoginRequest;
import com.agencyvoyage.web.dto.RegisterRequest;
import com.agencyvoyage.web.dto.TripResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Verifies the per-trip hotel catalog end to end. Logs in as the dev-seeded admin
 * account ({@code AdminUserSeeder}) rather than registering a fresh one, since there's
 * no public "become admin" endpoint by design - this also exercises the seeder itself.
 */
class HotelApiIT extends AbstractApiIT {

    private final RestTemplate rest = new RestTemplate();

    @Test
    void anAdminCanAddAHotelAndAnyoneCanListIt() {
        String adminToken = loginAsSeededAdmin();
        TripResponse trip = rest.getForObject(baseUrl() + "/api/trips", TripResponse[].class)[0];

        HotelResponse created = rest.exchange(
                        baseUrl() + "/api/trips/" + trip.id() + "/hotels",
                        HttpMethod.POST,
                        authed(
                                adminToken,
                                "{\"name\":\"Ubud Retreat\",\"description\":\"Jungle views\",\"photoUrls\":[\"https://x/a.jpg\"],\"amenities\":[\"Restaurant\",\"Pool\"]}"),
                        HotelResponse.class)
                .getBody();

        assertThat(created.name()).isEqualTo("Ubud Retreat");
        assertThat(created.photoUrls()).containsExactly("https://x/a.jpg");
        assertThat(created.amenities()).containsExactly("Restaurant", "Pool");

        HotelResponse[] hotels =
                rest.getForObject(baseUrl() + "/api/trips/" + trip.id() + "/hotels", HotelResponse[].class);
        assertThat(hotels).extracting(HotelResponse::name).contains("Ubud Retreat");
    }

    @Test
    void returns403WhenARegularUserTriesToAddAHotel() {
        String regularToken = registerAndLogin("NotAnAdmin");
        TripResponse trip = rest.getForObject(baseUrl() + "/api/trips", TripResponse[].class)[0];

        try {
            rest.exchange(
                    baseUrl() + "/api/trips/" + trip.id() + "/hotels",
                    HttpMethod.POST,
                    authed(regularToken, "{\"name\":\"Ubud Retreat\",\"description\":\"Jungle views\",\"photoUrls\":[]}"),
                    HotelResponse.class);
            org.junit.jupiter.api.Assertions.fail("expected a 403, the caller is not an admin");
        } catch (RestClientException e) {
            assertThat(e.getMessage()).contains("403");
        }
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

    private HttpEntity<String> authed(String token, String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new HttpEntity<>(jsonBody, headers);
    }
}
