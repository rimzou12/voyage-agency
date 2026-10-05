package com.agencyvoyage.web.controller;

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
import com.agencyvoyage.web.dto.AddHotelRequest;
import com.agencyvoyage.web.dto.HotelResponse;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HotelController {

    private final AddHotelUseCase addHotelUseCase;
    private final ListHotelsForTripUseCase listHotelsForTripUseCase;
    private final UpdateHotelUseCase updateHotelUseCase;
    private final DeleteHotelUseCase deleteHotelUseCase;

    public HotelController(
            AddHotelUseCase addHotelUseCase,
            ListHotelsForTripUseCase listHotelsForTripUseCase,
            UpdateHotelUseCase updateHotelUseCase,
            DeleteHotelUseCase deleteHotelUseCase) {
        this.addHotelUseCase = Objects.requireNonNull(addHotelUseCase);
        this.listHotelsForTripUseCase = Objects.requireNonNull(listHotelsForTripUseCase);
        this.updateHotelUseCase = Objects.requireNonNull(updateHotelUseCase);
        this.deleteHotelUseCase = Objects.requireNonNull(deleteHotelUseCase);
    }

    @PostMapping("/api/trips/{tripId}/hotels")
    public ResponseEntity<HotelResponse> addHotel(
            @PathVariable String tripId, @RequestBody AddHotelRequest request, @AuthenticationPrincipal User currentUser) {
        Hotel hotel = addHotelUseCase.addHotel(new AddHotelCommand(
                TripId.of(tripId),
                request.name(),
                request.description(),
                request.photoUrls(),
                request.amenities(),
                currentUser));
        return ResponseEntity.status(HttpStatus.CREATED).body(HotelResponse.from(hotel));
    }

    @GetMapping("/api/trips/{tripId}/hotels")
    public List<HotelResponse> listHotels(@PathVariable String tripId) {
        return listHotelsForTripUseCase.listHotels(TripId.of(tripId)).stream()
                .map(HotelResponse::from)
                .toList();
    }

    @PutMapping("/api/trips/{tripId}/hotels/{hotelId}")
    public HotelResponse updateHotel(
            @PathVariable String tripId,
            @PathVariable String hotelId,
            @RequestBody AddHotelRequest request,
            @AuthenticationPrincipal User currentUser) {
        Hotel hotel = updateHotelUseCase.updateHotel(new UpdateHotelCommand(
                HotelId.of(hotelId),
                request.name(),
                request.description(),
                request.photoUrls(),
                request.amenities(),
                currentUser));
        return HotelResponse.from(hotel);
    }

    @DeleteMapping("/api/trips/{tripId}/hotels/{hotelId}")
    public ResponseEntity<Void> deleteHotel(
            @PathVariable String tripId, @PathVariable String hotelId, @AuthenticationPrincipal User currentUser) {
        deleteHotelUseCase.deleteHotel(new DeleteHotelCommand(HotelId.of(hotelId), currentUser));
        return ResponseEntity.noContent().build();
    }
}
