package com.amugeonabuster.application.service;

import com.amugeonabuster.application.port.in.WelstoryMenuReviewUseCase;
import com.amugeonabuster.application.port.out.WelstoryMenuReviewPort;
import com.amugeonabuster.domain.model.WelstoryMenuReview;
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

    private final WelstoryMenuReviewPort reviewPort;

    @Override
    @Transactional
    public WelstoryMenuReview submitReview(SubmitReviewCommand command) {
        if (command.getRating() < 1 || command.getRating() > 5) {
            throw new IllegalArgumentException("별점은 1점부터 5점까지 선택해 주세요.");
        }
        if (command.getNickname() == null || command.getNickname().isBlank() || command.getNickname().length() > 20
                || command.getComment() == null || command.getComment().isBlank() || command.getComment().length() > 500
                || command.getCourseName() == null || command.getCourseName().isBlank()
                || command.getCafeteriaName() == null || command.getCafeteriaName().isBlank()
                || command.getUserFingerprint() == null || command.getUserFingerprint().isBlank()
                || command.getMenuDate() == null) {
            throw new IllegalArgumentException("이름과 한줄평 등 필수 입력을 확인해 주세요.");
        }
        // 도배 차단 (1 브라우저 세션당 특정 코스에 대해 당일 딱 1회만 등록 허용)
        boolean alreadySubmitted = reviewPort.existsByCafeteriaNameAndMenuDateAndCourseNameAndUserFingerprint(
            command.getCafeteriaName(),
            command.getMenuDate(),
            command.getCourseName(),
            command.getUserFingerprint()
        );

        if (alreadySubmitted) {
            throw new IllegalArgumentException("이미 오늘 해당 코스 식단에 후기를 등록하셨습니다.");
        }

        WelstoryMenuReview review = WelstoryMenuReview.builder()
            .cafeteriaName(command.getCafeteriaName())
            .menuDate(command.getMenuDate())
            .courseName(command.getCourseName())
            .menuDetails(command.getMenuDetails())
            .nickname(command.getNickname())
            .rating(command.getRating())
            .comment(command.getComment())
            .userFingerprint(command.getUserFingerprint())
            .createdAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul")))
            .build();

        log.info("Saving new Welstory review for cafeteria: {}, course: {}, rating: {} by {}",
            review.getCafeteriaName(), review.getCourseName(), review.getRating(), review.getNickname());

        return reviewPort.save(review);
    }

    @Override
    public List<WelstoryMenuReview> getReviews(String cafeteriaName, LocalDate menuDate) {
        return reviewPort.findAllByCafeteriaNameAndMenuDate(cafeteriaName, menuDate);
    }

    @Override
    public Map<String, CourseStats> getCourseStats(String cafeteriaName, LocalDate menuDate) {
        List<WelstoryMenuReview> reviews = reviewPort.findAllByCafeteriaNameAndMenuDate(cafeteriaName, menuDate);

        Map<String, List<WelstoryMenuReview>> grouped = reviews.stream()
            .collect(Collectors.groupingBy(WelstoryMenuReview::getCourseName));

        Map<String, CourseStats> statsMap = new HashMap<>();
        for (Map.Entry<String, List<WelstoryMenuReview>> entry : grouped.entrySet()) {
            double avg = entry.getValue().stream()
                .mapToInt(WelstoryMenuReview::getRating)
                .average()
                .orElse(0.0);
            int count = entry.getValue().size();
            statsMap.put(entry.getKey(), new CourseStats(Math.round(avg * 10.0) / 10.0, count));
        }
        return statsMap;
    }
}
