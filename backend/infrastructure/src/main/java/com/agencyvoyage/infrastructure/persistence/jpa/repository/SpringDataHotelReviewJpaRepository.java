package com.agencyvoyage.infrastructure.persistence.jpa.repository;

import com.agencyvoyage.infrastructure.persistence.jpa.entity.HotelReviewJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataHotelReviewJpaRepository extends JpaRepository<HotelReviewJpaEntity, UUID> {

    List<HotelReviewJpaEntity> findByHotelIdOrderByReviewedAtDesc(UUID hotelId);

    Optional<HotelReviewJpaEntity> findByHotelIdAndAuthorUserId(UUID hotelId, UUID authorUserId);
}
