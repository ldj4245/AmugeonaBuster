package com.amugeonabuster.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Builder
public class WelstoryMenuReview {
    private Long id;
    private String cafeteriaName;
    private LocalDate menuDate;
    private String courseName;
    private String menuDetails;
    private String nickname;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
    private String userFingerprint;
}
