package com.amugeonabuster.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@ToString
@Builder
public class WelstoryMenuReview {
    private final Long id;
    private final String cafeteriaName;
    private final LocalDate menuDate;
    private final String courseName;
    private final String menuDetails;
    private final String nickname;
    private final int rating;
    private final String comment;
    private final LocalDateTime createdAt;
    @ToString.Exclude
    private final String userFingerprint;
}
