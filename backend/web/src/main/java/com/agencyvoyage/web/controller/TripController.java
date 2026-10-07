package com.agencyvoyage.web.controller;

import com.agencyvoyage.application.port.in.CreateTripCommand;
import com.agencyvoyage.application.port.in.CreateTripUseCase;
import com.agencyvoyage.application.port.in.DeleteTripCommand;
import com.agencyvoyage.application.port.in.DeleteTripUseCase;
import com.agencyvoyage.application.port.in.GetTripUseCase;
import com.agencyvoyage.application.port.in.ListTripsUseCase;
import com.agencyvoyage.application.port.in.UpdateTripCommand;
import com.agencyvoyage.application.port.in.UpdateTripUseCase;
import com.agencyvoyage.domain.trip.PriceTier;
import com.agencyvoyage.domain.trip.Trip;
import com.agencyvoyage.domain.trip.TripId;
import com.agencyvoyage.domain.user.User;
import com.agencyvoyage.web.dto.TripRequest;
import com.agencyvoyage.web.dto.TripResponse;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final ListTripsUseCase listTripsUseCase;
    private final GetTripUseCase getTripUseCase;
    private final CreateTripUseCase createTripUseCase;
    private final UpdateTripUseCase updateTripUseCase;
    private final DeleteTripUseCase deleteTripUseCase;

    public TripController(
            ListTripsUseCase listTripsUseCase,
            GetTripUseCase getTripUseCase,
            CreateTripUseCase createTripUseCase,
            UpdateTripUseCase updateTripUseCase,
            DeleteTripUseCase deleteTripUseCase) {
        this.listTripsUseCase = Objects.requireNonNull(listTripsUseCase);
        this.getTripUseCase = Objects.requireNonNull(getTripUseCase);
        this.createTripUseCase = Objects.requireNonNull(createTripUseCase);
        this.updateTripUseCase = Objects.requireNonNull(updateTripUseCase);
        this.deleteTripUseCase = Objects.requireNonNull(deleteTripUseCase);
    }

    @GetMapping
    public List<TripResponse> listTrips() {
        return listTripsUseCase.listTrips().stream().map(TripResponse::from).toList();
    }

    @GetMapping("/{tripId}")
    public TripResponse getTrip(@PathVariable String tripId) {
        return TripResponse.from(getTripUseCase.getTrip(TripId.of(tripId)));
    }

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(
            @RequestBody TripRequest request, @AuthenticationPrincipal User currentUser) {
        Trip trip = createTripUseCase.createTrip(new CreateTripCommand(
                request.destination(),
                request.description(),
                request.departureDate(),
                request.returnDate(),
                request.minParticipants(),
                request.maxParticipants(),
                request.bookingDeadline(),
                request.basePrice(),
                toPriceTiers(request),
                request.photoUrls(),
                currentUser));
        return ResponseEntity.status(HttpStatus.CREATED).body(TripResponse.from(trip));
    }

    @PutMapping("/{tripId}")
    public TripResponse updateTrip(
            @PathVariable String tripId, @RequestBody TripRequest request, @AuthenticationPrincipal User currentUser) {
        Trip trip = updateTripUseCase.updateTrip(new UpdateTripCommand(
                TripId.of(tripId),
                request.destination(),
                request.description(),
                request.departureDate(),
                request.returnDate(),
                request.minParticipants(),
                request.maxParticipants(),
                request.bookingDeadline(),
                request.basePrice(),
                toPriceTiers(request),
                request.photoUrls(),
                currentUser));
        return TripResponse.from(trip);
    }

    @DeleteMapping("/{tripId}")
    public ResponseEntity<Void> deleteTrip(@PathVariable String tripId, @AuthenticationPrincipal User currentUser) {
        deleteTripUseCase.deleteTrip(new DeleteTripCommand(TripId.of(tripId), currentUser));
        return ResponseEntity.noContent().build();
    }

    private static List<PriceTier> toPriceTiers(TripRequest request) {
        return request.priceTiers() == null
                ? List.of()
                : request.priceTiers().stream()
                        .map(tier -> new PriceTier(tier.minParticipants(), tier.pricePerSeat()))
                        .toList();
    }
}
