package com.agencyvoyage.infrastructure.persistence.jpa.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "hotel")
public class HotelJpaEntity {

    @Id
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_photo_url", joinColumns = @JoinColumn(name = "hotel_id"))
    @OrderColumn(name = "position")
    @Column(name = "url", nullable = false)
    private List<String> photoUrls = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_amenity", joinColumns = @JoinColumn(name = "hotel_id"))
    @OrderColumn(name = "position")
    @Column(name = "amenity", nullable = false)
    private List<String> amenities = new ArrayList<>();

    protected HotelJpaEntity() {
        // JPA
    }

    public HotelJpaEntity(
            UUID id, UUID tripId, String name, String description, List<String> photoUrls, List<String> amenities) {
        this.id = id;
        this.tripId = tripId;
        this.name = name;
        this.description = description;
        this.photoUrls = new ArrayList<>(photoUrls);
        this.amenities = new ArrayList<>(amenities);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTripId() {
        return tripId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public List<String> getAmenities() {
        return amenities;
    }
}
