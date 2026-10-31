package com.amugeonabuster.adapter.in.web;

import com.amugeonabuster.application.port.in.WelstoryMenuReviewUseCase;
import com.amugeonabuster.domain.model.WelstoryMenuReview;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/welstory/reviews")
@RequiredArgsConstructor
public class WelstoryMenuReviewController {

    private static final String SESSION_REVIEW_FINGERPRINT = "reviewFingerprint";

    private final WelstoryMenuReviewUseCase reviewUseCase;

    public record SubmitReviewRequest(
            String cafeteriaName,
            String menuDate,
            String courseName,
            String menuDetails,
            String nickname,
            int rating,
            String comment,
            String userFingerprint
    ) {}

    public record ReviewResponse(
            Long id,
            String cafeteriaName,
            LocalDate menuDate,
            String courseName,
            String nickname,
            int rating,
            String comment,
            LocalDateTime createdAt
    ) {
        static ReviewResponse from(WelstoryMenuReview review) {
            return new ReviewResponse(
                    review.getId(),
                    review.getCafeteriaName(),
                    review.getMenuDate(),
                    review.getCourseName(),
                    review.getNickname(),
                    review.getRating(),
                    review.getComment(),
                    review.getCreatedAt()
            );
        }
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> submitReview(@RequestBody SubmitReviewRequest request, HttpSession session) {
        String fingerprint = resolveFingerprint(session, request.userFingerprint());
        WelstoryMenuReviewUseCase.SubmitReviewCommand command = WelstoryMenuReviewUseCase.SubmitReviewCommand.builder()
            .cafeteriaName(request.cafeteriaName())
            .menuDate(parseMenuDate(request.menuDate()))
            .courseName(request.courseName())
            .menuDetails(request.menuDetails())
            .nickname(request.nickname())
            .rating(request.rating())
            .comment(request.comment())
            .userFingerprint(fingerprint)
            .build();

        WelstoryMenuReview saved = reviewUseCase.submitReview(command);
        return ResponseEntity.ok(ReviewResponse.from(saved));
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(
            @RequestParam("cafeteriaName") String cafeteriaName,
            @RequestParam("menuDate") String menuDate) {
        List<ReviewResponse> responses = reviewUseCase.getReviews(cafeteriaName, parseMenuDate(menuDate)).stream()
                .map(ReviewResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, WelstoryMenuReviewUseCase.CourseStats>> getStats(
            @RequestParam("cafeteriaName") String cafeteriaName,
            @RequestParam("menuDate") String menuDate) {
        return ResponseEntity.ok(reviewUseCase.getCourseStats(cafeteriaName, parseMenuDate(menuDate)));
    }

    private static LocalDate parseMenuDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다.");
        }
        try {
            return LocalDate.parse(rawDate.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다.");
        }
    }

    private static String resolveFingerprint(HttpSession session, String requestedFingerprint) {
        Object existing = session.getAttribute(SESSION_REVIEW_FINGERPRINT);
        if (existing instanceof String bound && !bound.isBlank()) {
            return bound;
        }
        String candidate = (requestedFingerprint != null
                && !requestedFingerprint.isBlank()
                && requestedFingerprint.trim().length() <= 64)
                ? requestedFingerprint.trim()
                : UUID.randomUUID().toString();
        session.setAttribute(SESSION_REVIEW_FINGERPRINT, candidate);
        return candidate;
    }
}
