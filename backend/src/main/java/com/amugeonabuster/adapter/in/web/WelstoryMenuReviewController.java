package com.amugeonabuster.adapter.in.web;

import com.amugeonabuster.domain.model.WelstoryMenuReview;
import com.amugeonabuster.application.port.in.WelstoryMenuReviewUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/welstory/reviews")
@RequiredArgsConstructor
public class WelstoryMenuReviewController {

    private final WelstoryMenuReviewUseCase reviewUseCase;

    @PostMapping
    public ResponseEntity<?> submitReview(@RequestBody SubmitReviewRequest request) {
        try {
            WelstoryMenuReviewUseCase.SubmitReviewCommand command = WelstoryMenuReviewUseCase.SubmitReviewCommand.builder()
                .cafeteriaName(request.getCafeteriaName())
                .menuDate(LocalDate.parse(request.getMenuDate()))
                .courseName(request.getCourseName())
                .menuDetails(request.getMenuDetails())
                .nickname(request.getNickname())
                .rating(request.getRating())
                .comment(request.getComment())
                .userFingerprint(request.getUserFingerprint())
                .build();

            WelstoryMenuReview saved = reviewUseCase.submitReview(command);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<WelstoryMenuReview>> getReviews(
            @RequestParam("cafeteriaName") String cafeteriaName,
            @RequestParam("menuDate") String menuDate) {
        return ResponseEntity.ok(reviewUseCase.getReviews(cafeteriaName, LocalDate.parse(menuDate)));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, WelstoryMenuReviewUseCase.CourseStats>> getStats(
            @RequestParam("cafeteriaName") String cafeteriaName,
            @RequestParam("menuDate") String menuDate) {
        return ResponseEntity.ok(reviewUseCase.getCourseStats(cafeteriaName, LocalDate.parse(menuDate)));
    }

    @lombok.Data
    public static class SubmitReviewRequest {
        private String cafeteriaName;
        private String menuDate;
        private String courseName;
        private String menuDetails;
        private String nickname;
        private int rating;
        private String comment;
        private String userFingerprint;
    }
}
