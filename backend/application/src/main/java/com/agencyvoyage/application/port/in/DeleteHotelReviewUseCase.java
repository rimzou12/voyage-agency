package com.agencyvoyage.application.port.in;

/** The original author may delete their own review; an admin may delete any review. */
public interface DeleteHotelReviewUseCase {

    void deleteReview(DeleteHotelReviewCommand command);
}
