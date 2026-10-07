package com.agencyvoyage.infrastructure.persistence.jpa.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trip")
public class TripJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String destination;

    @Column(length = 2000)
    private String description;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "min_participants", nullable = false)
    private int minParticipants;

    @Column(name = "max_participants", nullable = false)
    private int maxParticipants;

    @Column(name = "booking_deadline", nullable = false)
    private Instant bookingDeadline;

    @Column(name = "base_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal basePrice;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "trip_price_tier", joinColumns = @JoinColumn(name = "trip_id"))
    @OrderBy("minParticipants ASC")
    private List<PriceTierEmbeddable> priceTiers = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "trip_photo_url", joinColumns = @JoinColumn(name = "trip_id"))
    @OrderColumn(name = "position")
    @Column(name = "url", nullable = false)
    private List<String> photoUrls = new ArrayList<>();

    protected TripJpaEntity() {
        // JPA
    }

    public TripJpaEntity(
            UUID id,
            String destination,
            String description,
            LocalDate departureDate,
            LocalDate returnDate,
            int minParticipants,
            int maxParticipants,
            Instant bookingDeadline,
            BigDecimal basePrice,
            List<PriceTierEmbeddable> priceTiers,
            List<String> photoUrls) {
        this.id = id;
        this.destination = destination;
        this.description = description;
        this.departureDate = departureDate;
        this.returnDate = returnDate;
        this.minParticipants = minParticipants;
        this.maxParticipants = maxParticipants;
        this.bookingDeadline = bookingDeadline;
        this.basePrice = basePrice;
        this.priceTiers = new ArrayList<>(priceTiers);
        this.photoUrls = new ArrayList<>(photoUrls);
    }

    public UUID getId() {
        return id;
    }

    public String getDestination() {
        return destination;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public int getMinParticipants() {
        return minParticipants;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public Instant getBookingDeadline() {
        return bookingDeadline;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public List<PriceTierEmbeddable> getPriceTiers() {
        return priceTiers;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }
}
