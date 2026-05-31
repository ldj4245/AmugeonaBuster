package com.amugeonabuster.application.port.out;

import com.amugeonabuster.domain.model.WelstoryMenuReview;

import java.time.LocalDate;
import java.util.List;

public interface WelstoryMenuReviewPort {
    WelstoryMenuReview save(WelstoryMenuReview review);
    List<WelstoryMenuReview> findAllByCafeteriaNameAndMenuDate(String cafeteriaName, LocalDate menuDate);
    boolean existsByCafeteriaNameAndMenuDateAndCourseNameAndUserFingerprint(String cafeteriaName, LocalDate menuDate, String courseName, String userFingerprint);
}
