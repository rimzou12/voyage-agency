package com.agencyvoyage.application.service;

import com.agencyvoyage.application.exception.HotelNotFoundException;
import com.agencyvoyage.application.exception.NotAnAdminException;
import com.agencyvoyage.application.port.in.UpdateHotelCommand;
import com.agencyvoyage.application.port.in.UpdateHotelUseCase;
import com.agencyvoyage.application.port.out.HotelRepository;
import com.agencyvoyage.domain.hotel.Hotel;
import java.util.Objects;

public final class UpdateHotelService implements UpdateHotelUseCase {

    private final HotelRepository hotelRepository;

    public UpdateHotelService(HotelRepository hotelRepository) {
        this.hotelRepository = Objects.requireNonNull(hotelRepository, "hotelRepository must not be null");
    }

    @Override
    public Hotel updateHotel(UpdateHotelCommand command) {
        if (!command.requestedBy().isAdmin()) {
            throw new NotAnAdminException();
        }
        Hotel existing = hotelRepository
                .findById(command.hotelId())
                .orElseThrow(() -> new HotelNotFoundException(command.hotelId()));

        Hotel updated = new Hotel(
                existing.id(),
                existing.tripId(),
                command.name(),
                command.description(),
                command.photoUrls(),
                command.amenities());
        hotelRepository.save(updated);
        return updated;
    }
}
