package com.agencyvoyage.infrastructure.persistence.jpa.adapter;

import com.agencyvoyage.application.port.out.HotelReviewRepository;
import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.hotel.HotelReview;
import com.agencyvoyage.domain.hotel.HotelReviewId;
import com.agencyvoyage.domain.user.UserId;
import com.agencyvoyage.infrastructure.persistence.jpa.entity.HotelReviewJpaEntity;
import com.agencyvoyage.infrastructure.persistence.jpa.repository.SpringDataHotelReviewJpaRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class HotelReviewRepositoryAdapter implements HotelReviewRepository {

    private final SpringDataHotelReviewJpaRepository springDataRepository;

    public HotelReviewRepositoryAdapter(SpringDataHotelReviewJpaRepository springDataRepository) {
        this.springDataRepository =
                Objects.requireNonNull(springDataRepository, "springDataRepository must not be null");
    }

    @Override
    public void save(HotelReview review) {
        springDataRepository.save(new HotelReviewJpaEntity(
                review.id().value(),
                review.hotelId().value(),
                review.authorId().value(),
                review.authorName(),
                review.rating(),
                review.comment(),
                review.reviewedAt()));
    }

    @Override
    public Optional<HotelReview> findById(HotelReviewId id) {
        return springDataRepository.findById(id.value()).map(HotelReviewRepositoryAdapter::toDomain);
    }

    @Override
    public List<HotelReview> findByHotelId(HotelId hotelId) {
        return springDataRepository.findByHotelIdOrderByReviewedAtDesc(hotelId.value()).stream()
                .map(HotelReviewRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<HotelReview> findByHotelIdAndAuthorId(HotelId hotelId, UserId authorId) {
        return springDataRepository
                .findByHotelIdAndAuthorUserId(hotelId.value(), authorId.value())
                .map(HotelReviewRepositoryAdapter::toDomain);
    }

    @Override
    public void deleteById(HotelReviewId id) {
        springDataRepository.deleteById(id.value());
    }

    private static HotelReview toDomain(HotelReviewJpaEntity entity) {
        return new HotelReview(
                new HotelReviewId(entity.getId()),
                new HotelId(entity.getHotelId()),
                new UserId(entity.getAuthorUserId()),
                entity.getAuthorName(),
                entity.getRating(),
                entity.getComment(),
                entity.getReviewedAt());
    }
}
