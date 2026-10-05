package com.agencyvoyage.web.controller;

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
import com.agencyvoyage.web.dto.AddHotelReviewRequest;
import com.agencyvoyage.web.dto.HotelReviewResponse;
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
public class HotelReviewController {

    private final AddHotelReviewUseCase addHotelReviewUseCase;
    private final ListHotelReviewsUseCase listHotelReviewsUseCase;
    private final UpdateHotelReviewUseCase updateHotelReviewUseCase;
    private final DeleteHotelReviewUseCase deleteHotelReviewUseCase;

    public HotelReviewController(
            AddHotelReviewUseCase addHotelReviewUseCase,
            ListHotelReviewsUseCase listHotelReviewsUseCase,
            UpdateHotelReviewUseCase updateHotelReviewUseCase,
            DeleteHotelReviewUseCase deleteHotelReviewUseCase) {
        this.addHotelReviewUseCase = Objects.requireNonNull(addHotelReviewUseCase);
        this.listHotelReviewsUseCase = Objects.requireNonNull(listHotelReviewsUseCase);
        this.updateHotelReviewUseCase = Objects.requireNonNull(updateHotelReviewUseCase);
        this.deleteHotelReviewUseCase = Objects.requireNonNull(deleteHotelReviewUseCase);
    }

    @PostMapping("/api/hotels/{hotelId}/reviews")
    public ResponseEntity<HotelReviewResponse> addReview(
            @PathVariable String hotelId,
            @RequestBody AddHotelReviewRequest request,
            @AuthenticationPrincipal User currentUser) {
        HotelReview review = addHotelReviewUseCase.addReview(
                new AddHotelReviewCommand(HotelId.of(hotelId), request.rating(), request.comment(), currentUser));
        return ResponseEntity.status(HttpStatus.CREATED).body(HotelReviewResponse.from(review));
    }

    @GetMapping("/api/hotels/{hotelId}/reviews")
    public List<HotelReviewResponse> listReviews(@PathVariable String hotelId) {
        return listHotelReviewsUseCase.listReviews(HotelId.of(hotelId)).stream()
                .map(HotelReviewResponse::from)
                .toList();
    }

    @PutMapping("/api/hotels/{hotelId}/reviews/{reviewId}")
    public HotelReviewResponse updateReview(
            @PathVariable String hotelId,
            @PathVariable String reviewId,
            @RequestBody AddHotelReviewRequest request,
            @AuthenticationPrincipal User currentUser) {
        HotelReview review = updateHotelReviewUseCase.updateReview(new UpdateHotelReviewCommand(
                HotelReviewId.of(reviewId), request.rating(), request.comment(), currentUser));
        return HotelReviewResponse.from(review);
    }

    @DeleteMapping("/api/hotels/{hotelId}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable String hotelId, @PathVariable String reviewId, @AuthenticationPrincipal User currentUser) {
        deleteHotelReviewUseCase.deleteReview(new DeleteHotelReviewCommand(HotelReviewId.of(reviewId), currentUser));
        return ResponseEntity.noContent().build();
    }
}
