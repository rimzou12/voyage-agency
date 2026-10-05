package com.agencyvoyage.application.exception;

public final class NotReviewAuthorException extends RuntimeException {

    public NotReviewAuthorException() {
        super("You may only edit or delete your own review");
    }
}
