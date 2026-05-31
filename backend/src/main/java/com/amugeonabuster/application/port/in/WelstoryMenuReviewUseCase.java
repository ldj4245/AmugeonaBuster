package com.amugeonabuster.application.port.in;

import com.amugeonabuster.domain.model.WelstoryMenuReview;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface WelstoryMenuReviewUseCase {
    WelstoryMenuReview submitReview(SubmitReviewCommand command);
    List<WelstoryMenuReview> getReviews(String cafeteriaName, LocalDate menuDate);
    Map<String, CourseStats> getCourseStats(String cafeteriaName, LocalDate menuDate);

    @lombok.Value
    @lombok.Builder
    class SubmitReviewCommand {
        String cafeteriaName;
        LocalDate menuDate;
        String courseName;
        String menuDetails;
        String nickname;
        int rating;
        String comment;
        String userFingerprint;
    }

    @lombok.Value
    class CourseStats {
        double averageRating;
        int reviewCount;
    }
}
