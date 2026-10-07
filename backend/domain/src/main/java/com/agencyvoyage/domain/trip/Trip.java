package com.agencyvoyage.domain.trip;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * A travel package an agency offers, bookable as a group: the more participants join
 * a {@link com.agencyvoyage.domain.booking.GroupBooking} for this trip, the cheaper
 * the seat becomes, down to the tiers defined here.
 */
public final class Trip {

    private final TripId id;
    private final String destination;
    private final String description;
    private final LocalDate departureDate;
    private final LocalDate returnDate;
    private final int minParticipants;
    private final int maxParticipants;
    private final Instant bookingDeadline;
    private final PricingSchedule pricingSchedule;
    private final List<String> photoUrls;

    public Trip(
            TripId id,
            String destination,
            String description,
            LocalDate departureDate,
            LocalDate returnDate,
            int minParticipants,
            int maxParticipants,
            Instant bookingDeadline,
            PricingSchedule pricingSchedule,
            List<String> photoUrls) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.destination = requireNonBlank(destination, "destination");
        this.description = description == null ? "" : description;
        this.departureDate = Objects.requireNonNull(departureDate, "departureDate must not be null");
        this.returnDate = Objects.requireNonNull(returnDate, "returnDate must not be null");
        this.bookingDeadline = Objects.requireNonNull(bookingDeadline, "bookingDeadline must not be null");
        this.pricingSchedule = Objects.requireNonNull(pricingSchedule, "pricingSchedule must not be null");
        this.photoUrls = photoUrls == null ? List.of() : List.copyOf(photoUrls);

        if (!departureDate.isBefore(returnDate)) {
            throw new IllegalArgumentException("departureDate must be before returnDate");
        }
        if (minParticipants < 1) {
            throw new IllegalArgumentException("minParticipants must be at least 1, got " + minParticipants);
        }
        if (maxParticipants < minParticipants) {
            throw new IllegalArgumentException(
                    "maxParticipants (" + maxParticipants + ") must be >= minParticipants (" + minParticipants + ")");
        }
        this.minParticipants = minParticipants;
        this.maxParticipants = maxParticipants;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    public boolean acceptsNewGroupBookingsAt(Instant now) {
        return now.isBefore(bookingDeadline);
    }

    public TripId id() {
        return id;
    }

    public String destination() {
        return destination;
    }

    public String description() {
        return description;
    }

    public LocalDate departureDate() {
        return departureDate;
    }

    public LocalDate returnDate() {
        return returnDate;
    }

    public int minParticipants() {
        return minParticipants;
    }

    public int maxParticipants() {
        return maxParticipants;
    }

    public Instant bookingDeadline() {
        return bookingDeadline;
    }

    public PricingSchedule pricingSchedule() {
        return pricingSchedule;
    }

    public List<PriceTier> priceTiers() {
        return pricingSchedule.tiers();
    }

    public List<String> photoUrls() {
        return photoUrls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trip trip)) return false;
        return id.equals(trip.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
