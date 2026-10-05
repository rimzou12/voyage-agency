package com.agencyvoyage.application.service;

import com.agencyvoyage.application.exception.NotAnAdminException;
import com.agencyvoyage.application.port.in.AddHotelCommand;
import com.agencyvoyage.application.port.in.AddHotelUseCase;
import com.agencyvoyage.application.port.out.HotelRepository;
import com.agencyvoyage.domain.hotel.Hotel;
import com.agencyvoyage.domain.hotel.HotelId;
import java.util.Objects;

public final class AddHotelService implements AddHotelUseCase {

    private final HotelRepository hotelRepository;

    public AddHotelService(HotelRepository hotelRepository) {
        this.hotelRepository = Objects.requireNonNull(hotelRepository, "hotelRepository must not be null");
    }

    @Override
    public Hotel addHotel(AddHotelCommand command) {
        if (!command.requestedBy().isAdmin()) {
            throw new NotAnAdminException();
        }

        Hotel hotel = new Hotel(
                HotelId.newId(),
                command.tripId(),
                command.name(),
                command.description(),
                command.photoUrls(),
                command.amenities());
        hotelRepository.save(hotel);
        return hotel;
    }
}
