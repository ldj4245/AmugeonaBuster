package com.amugeonabuster.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Setter
@ToString
@Builder
public class WelstoryAlertSettings {
    private Long id;
    private String kakaoId;
    private String nickname;
    @ToString.Exclude
    private String kakaoAccessToken;
    @ToString.Exclude
    private String kakaoRefreshToken;
    private String cotNo;
    private String hallNo;
    private String cafeteriaName;
    private String scheduledDays; // e.g., "1,2,3,4,5"
    private String scheduledTime; // e.g., "11:30"
    private boolean isEnabled;
    private LocalDate lastSentDate;
}
