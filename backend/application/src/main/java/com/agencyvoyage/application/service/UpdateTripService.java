package com.agencyvoyage.application.service;

import com.agencyvoyage.application.exception.NotAnAdminException;
import com.agencyvoyage.application.exception.TripNotFoundException;
import com.agencyvoyage.application.port.in.UpdateTripCommand;
import com.agencyvoyage.application.port.in.UpdateTripUseCase;
import com.agencyvoyage.application.port.out.TripRepository;
import com.agencyvoyage.domain.trip.PricingSchedule;
import com.agencyvoyage.domain.trip.Trip;
import java.util.Objects;

public final class UpdateTripService implements UpdateTripUseCase {

    private final TripRepository tripRepository;

    public UpdateTripService(TripRepository tripRepository) {
        this.tripRepository = Objects.requireNonNull(tripRepository, "tripRepository must not be null");
    }

    @Override
    public Trip updateTrip(UpdateTripCommand command) {
        if (!command.requestedBy().isAdmin()) {
            throw new NotAnAdminException();
        }
        tripRepository
                .findById(command.tripId())
                .orElseThrow(() -> new TripNotFoundException(command.tripId()));

        PricingSchedule schedule =
                PricingSchedule.of(command.basePrice(), command.priceTiers(), command.maxParticipants());
        Trip trip = new Trip(
                command.tripId(),
                command.destination(),
                command.description(),
                command.departureDate(),
                command.returnDate(),
                command.minParticipants(),
                command.maxParticipants(),
                command.bookingDeadline(),
                schedule,
                command.photoUrls());
        tripRepository.save(trip);
        return trip;
    }
}
