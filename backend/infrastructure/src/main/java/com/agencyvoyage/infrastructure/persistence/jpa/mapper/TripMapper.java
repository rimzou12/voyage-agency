package com.agencyvoyage.infrastructure.persistence.jpa.mapper;

import com.agencyvoyage.domain.trip.PriceTier;
import com.agencyvoyage.domain.trip.PricingSchedule;
import com.agencyvoyage.domain.trip.Trip;
import com.agencyvoyage.domain.trip.TripId;
import com.agencyvoyage.infrastructure.persistence.jpa.entity.PriceTierEmbeddable;
import com.agencyvoyage.infrastructure.persistence.jpa.entity.TripJpaEntity;
import java.util.List;

public final class TripMapper {

    private TripMapper() {
    }

    public static TripJpaEntity toEntity(Trip trip) {
        List<PriceTierEmbeddable> tiers = trip.priceTiers().stream()
                .map(tier -> new PriceTierEmbeddable(tier.minParticipants(), tier.pricePerSeat()))
                .toList();
        return new TripJpaEntity(
                trip.id().value(),
                trip.destination(),
                trip.description(),
                trip.departureDate(),
                trip.returnDate(),
                trip.minParticipants(),
                trip.maxParticipants(),
                trip.bookingDeadline(),
                trip.pricingSchedule().basePrice(),
                tiers,
                trip.photoUrls());
    }

    public static Trip toDomain(TripJpaEntity entity) {
        List<PriceTier> tiers = entity.getPriceTiers().stream()
                .map(tier -> new PriceTier(tier.getMinParticipants(), tier.getPricePerSeat()))
                .toList();
        PricingSchedule schedule = PricingSchedule.of(entity.getBasePrice(), tiers, entity.getMaxParticipants());
        return new Trip(
                new TripId(entity.getId()),
                entity.getDestination(),
                entity.getDescription(),
                entity.getDepartureDate(),
                entity.getReturnDate(),
                entity.getMinParticipants(),
                entity.getMaxParticipants(),
                entity.getBookingDeadline(),
                schedule,
                entity.getPhotoUrls());
    }
}
