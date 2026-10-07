package com.agencyvoyage.application.service;

import com.agencyvoyage.application.exception.NotAnAdminException;
import com.agencyvoyage.application.port.in.CreateTripCommand;
import com.agencyvoyage.application.port.in.CreateTripUseCase;
import com.agencyvoyage.application.port.out.TripRepository;
import com.agencyvoyage.domain.trip.PricingSchedule;
import com.agencyvoyage.domain.trip.Trip;
import com.agencyvoyage.domain.trip.TripId;
import java.util.Objects;

public final class CreateTripService implements CreateTripUseCase {

    private final TripRepository tripRepository;

    public CreateTripService(TripRepository tripRepository) {
        this.tripRepository = Objects.requireNonNull(tripRepository, "tripRepository must not be null");
    }

    @Override
    public Trip createTrip(CreateTripCommand command) {
        if (!command.requestedBy().isAdmin()) {
            throw new NotAnAdminException();
        }

        PricingSchedule schedule =
                PricingSchedule.of(command.basePrice(), command.priceTiers(), command.maxParticipants());
        Trip trip = new Trip(
                TripId.newId(),
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
