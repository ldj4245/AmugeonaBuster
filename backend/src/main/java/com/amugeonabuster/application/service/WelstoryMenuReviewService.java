package com.amugeonabuster.application.service;

import com.amugeonabuster.adapter.out.persistence.SpringDataWelstoryMenuReviewRepository;
import com.amugeonabuster.adapter.out.persistence.WelstoryMenuReviewJpaEntity;
import com.amugeonabuster.application.port.in.WelstoryMenuReviewUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WelstoryMenuReviewService implements WelstoryMenuReviewUseCase {

    private final SpringDataWelstoryMenuReviewRepository repository;

    @Override
    @Transactional
    public WelstoryMenuReviewJpaEntity submitReview(SubmitReviewCommand command) {
        // 도배 차단 (1 브라우저 세션당 특정 코스에 대해 당일 딱 1회만 등록 허용)
        boolean alreadySubmitted = repository.existsByCafeteriaNameAndMenuDateAndCourseNameAndUserFingerprint(
            command.getCafeteriaName(),
            command.getMenuDate(),
            command.getCourseName(),
            command.getUserFingerprint()
        );

        if (alreadySubmitted) {
            throw new IllegalArgumentException("이미 오늘 해당 코스 식단에 후기를 등록하셨습니다.");
        }

        WelstoryMenuReviewJpaEntity entity = WelstoryMenuReviewJpaEntity.builder()
            .cafeteriaName(command.getCafeteriaName())
            .menuDate(command.getMenuDate())
            .courseName(command.getCourseName())
            .menuDetails(command.getMenuDetails())
            .nickname(command.getNickname())
            .rating(command.getRating())
            .comment(command.getComment())
            .userFingerprint(command.getUserFingerprint())
            .createdAt(LocalDateTime.now())
            .build();

        log.info("Saving new Welstory review for cafeteria: {}, course: {}, rating: {} by {}",
            entity.getCafeteriaName(), entity.getCourseName(), entity.getRating(), entity.getNickname());

        return repository.save(entity);
    }

    @Override
    public List<WelstoryMenuReviewJpaEntity> getReviews(String cafeteriaName, LocalDate menuDate) {
        return repository.findAllByCafeteriaNameAndMenuDateOrderByCreatedAtDesc(cafeteriaName, menuDate);
    }

    @Override
    public Map<String, CourseStats> getCourseStats(String cafeteriaName, LocalDate menuDate) {
        List<WelstoryMenuReviewJpaEntity> reviews = repository.findAllByCafeteriaNameAndMenuDateOrderByCreatedAtDesc(cafeteriaName, menuDate);

        Map<String, List<WelstoryMenuReviewJpaEntity>> grouped = reviews.stream()
            .collect(Collectors.groupingBy(WelstoryMenuReviewJpaEntity::getCourseName));

        Map<String, CourseStats> statsMap = new HashMap<>();
        for (Map.Entry<String, List<WelstoryMenuReviewJpaEntity>> entry : grouped.entrySet()) {
            double avg = entry.getValue().stream()
                .mapToInt(WelstoryMenuReviewJpaEntity::getRating)
                .average()
                .orElse(0.0);
            int count = entry.getValue().size();
            statsMap.put(entry.getKey(), new CourseStats(Math.round(avg * 10.0) / 10.0, count));
        }
        return statsMap;
    }
}
