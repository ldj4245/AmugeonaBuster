package com.amugeonabuster.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "welstory_alert_settings", indexes = {
    @Index(name = "idx_welstory_schedule", columnList = "scheduledTime, isEnabled")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WelstoryAlertSettingsJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String kakaoId;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false, length = 500)
    private String kakaoAccessToken;

    @Column(nullable = false, length = 500)
    private String kakaoRefreshToken;

    @Column(nullable = false)
    private String cotNo;

    @Column(nullable = false)
    private String hallNo;

    @Column(nullable = false)
    private String cafeteriaName;

    @Column(nullable = false)
    private String scheduledDays; // e.g., "1,2,3,4,5" (Monday=1, Sunday=7)

    @Column(nullable = false)
    private String scheduledTime; // e.g., "11:30"

    @Column(nullable = false)
    private boolean isEnabled;

    @Column
    private LocalDate lastSentDate; // To avoid multiple sends in a single day
}
