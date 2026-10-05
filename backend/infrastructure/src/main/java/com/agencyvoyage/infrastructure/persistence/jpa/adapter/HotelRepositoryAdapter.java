package com.agencyvoyage.infrastructure.persistence.jpa.adapter;

import com.agencyvoyage.application.port.out.HotelRepository;
import com.agencyvoyage.domain.hotel.Hotel;
import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.trip.TripId;
import com.agencyvoyage.infrastructure.persistence.jpa.entity.HotelJpaEntity;
import com.agencyvoyage.infrastructure.persistence.jpa.repository.SpringDataHotelJpaRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class HotelRepositoryAdapter implements HotelRepository {

    private final SpringDataHotelJpaRepository springDataRepository;

    public HotelRepositoryAdapter(SpringDataHotelJpaRepository springDataRepository) {
        this.springDataRepository =
                Objects.requireNonNull(springDataRepository, "springDataRepository must not be null");
    }

    @Override
    public void save(Hotel hotel) {
        springDataRepository.save(new HotelJpaEntity(
                hotel.id().value(),
                hotel.tripId().value(),
                hotel.name(),
                hotel.description(),
                hotel.photoUrls(),
                hotel.amenities()));
    }

    @Override
    public Optional<Hotel> findById(HotelId id) {
        return springDataRepository.findById(id.value()).map(HotelRepositoryAdapter::toDomain);
    }

    @Override
    public List<Hotel> findByTripId(TripId tripId) {
        return springDataRepository.findByTripId(tripId.value()).stream()
                .map(HotelRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public void deleteById(HotelId id) {
        springDataRepository.deleteById(id.value());
    }

    private static Hotel toDomain(HotelJpaEntity entity) {
        return new Hotel(
                new HotelId(entity.getId()),
                new TripId(entity.getTripId()),
                entity.getName(),
                entity.getDescription(),
                entity.getPhotoUrls(),
                entity.getAmenities());
    }
}
