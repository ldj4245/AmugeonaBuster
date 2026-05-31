package com.amugeonabuster.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface SpringDataWelstoryMenuReviewRepository extends JpaRepository<WelstoryMenuReviewJpaEntity, Long> {
    List<WelstoryMenuReviewJpaEntity> findAllByCafeteriaNameAndMenuDateOrderByCreatedAtDesc(String cafeteriaName, LocalDate menuDate);
    boolean existsByCafeteriaNameAndMenuDateAndCourseNameAndUserFingerprint(String cafeteriaName, LocalDate menuDate, String courseName, String userFingerprint);
}
