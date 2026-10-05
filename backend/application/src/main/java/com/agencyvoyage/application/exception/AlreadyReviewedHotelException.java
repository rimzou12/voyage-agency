package com.agencyvoyage.application.exception;

public final class AlreadyReviewedHotelException extends RuntimeException {

    public AlreadyReviewedHotelException() {
        super("You have already reviewed this hotel - edit your existing review instead");
    }
}
