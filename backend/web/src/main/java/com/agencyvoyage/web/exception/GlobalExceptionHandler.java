package com.agencyvoyage.web.exception;

import com.agencyvoyage.application.exception.AlreadyReviewedHotelException;
import com.agencyvoyage.application.exception.ConversationAccessDeniedException;
import com.agencyvoyage.application.exception.EmailAlreadyRegisteredException;
import com.agencyvoyage.application.exception.GroupBookingNotFoundException;
import com.agencyvoyage.application.exception.HotelNotFoundException;
import com.agencyvoyage.application.exception.HotelReviewNotFoundException;
import com.agencyvoyage.application.exception.InvalidCredentialsException;
import com.agencyvoyage.application.exception.NotAnAdminException;
import com.agencyvoyage.application.exception.NotReviewAuthorException;
import com.agencyvoyage.application.exception.TripNotFoundException;
import com.agencyvoyage.domain.exception.DomainException;
import com.agencyvoyage.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
        TripNotFoundException.class,
        GroupBookingNotFoundException.class,
        HotelNotFoundException.class,
        HotelReviewNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({EmailAlreadyRegisteredException.class, AlreadyReviewedHotelException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({NotAnAdminException.class, NotReviewAuthorException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ConversationAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleConversationAccessDenied(ConversationAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainRuleViolation(DomainException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }
}
