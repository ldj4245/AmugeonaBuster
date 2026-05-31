package com.amugeonabuster.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "welstory_menu_reviews", indexes = {
    @Index(name = "idx_review_lookup", columnList = "cafeteriaName, menuDate")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WelstoryMenuReviewJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cafeteriaName; // e.g., "DSR 3F 식당"

    @Column(nullable = false)
    private LocalDate menuDate; // 식단 일자 (e.g., 2026-05-31)

    @Column(nullable = false)
    private String courseName; // 코스명 (e.g., "점심 - A코스 (KOREAN)")

    @Column(length = 500)
    private String menuDetails; // 상세 반찬 내역 백업

    @Column(nullable = false)
    private String nickname; // 작성자 닉네임

    @Column(nullable = false)
    private int rating; // 별점 (1 ~ 5)

    @Column(nullable = false, length = 1000)
    private String comment; // 한줄평 후기 내용

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String userFingerprint; // 브라우저 고유식별 키 (도배 차단용)
}
