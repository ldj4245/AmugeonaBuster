package com.amugeonabuster.adapter.out.persistence;

import com.amugeonabuster.application.port.out.WelstoryMenuReviewPort;
import com.amugeonabuster.domain.model.WelstoryMenuReview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WelstoryMenuReviewPersistenceAdapter implements WelstoryMenuReviewPort {

    private final SpringDataWelstoryMenuReviewRepository repository;

    @Override
    public WelstoryMenuReview save(WelstoryMenuReview review) {
        WelstoryMenuReviewJpaEntity entity = toEntity(review);
        WelstoryMenuReviewJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<WelstoryMenuReview> findAllByCafeteriaNameAndMenuDate(String cafeteriaName, LocalDate menuDate) {
        return repository.findAllByCafeteriaNameAndMenuDateOrderByCreatedAtDesc(cafeteriaName, menuDate).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCafeteriaNameAndMenuDateAndCourseNameAndUserFingerprint(String cafeteriaName, LocalDate menuDate, String courseName, String userFingerprint) {
        return repository.existsByCafeteriaNameAndMenuDateAndCourseNameAndUserFingerprint(cafeteriaName, menuDate, courseName, userFingerprint);
    }

    private WelstoryMenuReview toDomain(WelstoryMenuReviewJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return WelstoryMenuReview.builder()
            .id(entity.getId())
            .cafeteriaName(entity.getCafeteriaName())
            .menuDate(entity.getMenuDate())
            .courseName(entity.getCourseName())
            .menuDetails(entity.getMenuDetails())
            .nickname(entity.getNickname())
            .rating(entity.getRating())
            .comment(entity.getComment())
            .createdAt(entity.getCreatedAt())
            .userFingerprint(entity.getUserFingerprint())
            .build();
    }

    private WelstoryMenuReviewJpaEntity toEntity(WelstoryMenuReview domain) {
        if (domain == null) {
            return null;
        }
        return WelstoryMenuReviewJpaEntity.builder()
            .id(domain.getId())
            .cafeteriaName(domain.getCafeteriaName())
            .menuDate(domain.getMenuDate())
            .courseName(domain.getCourseName())
            .menuDetails(domain.getMenuDetails())
            .nickname(domain.getNickname())
            .rating(domain.getRating())
            .comment(domain.getComment())
            .createdAt(domain.getCreatedAt())
            .userFingerprint(domain.getUserFingerprint())
            .build();
    }
}
