package com.amugeonabuster.application.service;

import com.amugeonabuster.domain.model.WelstoryAlertSettings;
import com.amugeonabuster.application.port.out.WelstoryAlertPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class WelstoryScheduler {

    private final WelstoryAlertPort welstoryAlertPort;
    private final WelstoryMenuService menuService;
    private final KakaoMessageService kakaoMessageService;

    /**
     * 매 분 0초마다 구동되는 배치 스케줄러입니다.
     * 현재 분에 매칭된 유저들을 조회하여 오늘의 웰스토리 식단을 전송합니다.
     */
    @Scheduled(cron = "0 * * * * *")
    public void runScheduledWelstoryAlerts() {
        java.time.ZoneId seoulZone = java.time.ZoneId.of("Asia/Seoul");
        LocalTime now = LocalTime.now(seoulZone);
        String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        LocalDate today = LocalDate.now(seoulZone);
        int todayDayOfWeek = today.getDayOfWeek().getValue(); // 1 (Monday) ~ 7 (Sunday)

        log.info("Welstory Scheduled alert checking at time: {}, DayOfWeek: {} (KST forced)", currentTime, todayDayOfWeek);

        List<WelstoryAlertSettings> activeSettings = welstoryAlertPort.findAllByScheduledTimeAndIsEnabled(currentTime);
        if (activeSettings.isEmpty()) {
            return;
        }

        log.info("Found {} active Welstory alert configurations for time {}", activeSettings.size(), currentTime);

        for (WelstoryAlertSettings setting : activeSettings) {
            try {
                // 1. 오늘 날짜에 발송이 예약되어 있는지 확인 (scheduledDays 예: "1,2,3,4,5")
                List<String> days = Arrays.asList(setting.getScheduledDays().split(","));
                if (!days.contains(String.valueOf(todayDayOfWeek))) {
                    continue; // 오늘은 알림 제외인 요일
                }

                // 2. 오늘 이미 발송을 완료했는지 체크 (하루 중복 발송 차단)
                if (today.equals(setting.getLastSentDate())) {
                    log.info("User {} already received today's Welstory alert. Skipping.", setting.getNickname());
                    continue;
                }

                // 3. 발송 처리 진행
                processAlertSending(setting, today);

            } catch (Exception e) {
                log.error("Error processing scheduled Welstory alert for user {}: {}", setting.getNickname(), e.getMessage(), e);
            }
        }
    }

    /**
     * 개별 유저에 대한 식단 크롤링 및 발송 로직 수행 (OAuth 토큰 갱신 보장형)
     */
    private void processAlertSending(WelstoryAlertSettings setting, LocalDate today) {
        log.info("Sending scheduled Welstory alert to user {}", setting.getNickname());

        // 1. 식단 정보 긁어오기
        WelstoryMenuService.WelstoryMenuResult menu = menuService.getTodayMenu(
            setting.getCotNo(), 
            setting.getHallNo(), 
            setting.getCafeteriaName()
        );

        // 2. 알림 설정 시간에 해당하는 식단만 필터링!
        WelstoryMenuService.WelstoryMenuResult filteredMenu = menuService.filterMenuByTime(menu, setting.getScheduledTime());

        // 3. 카카오톡 메시지 전송 시도
        boolean success = kakaoMessageService.sendWelstoryMenuToMe(setting.getKakaoAccessToken(), filteredMenu, setting.getCotNo(), setting.getHallNo());
        
        if (!success) {
            // 💡 발송 실패 시 토큰 만료(401)일 확률이 높으므로 즉시 Refresh Token으로 자동 갱신 시도!
            log.info("Failed to send message to user {}. Attempting to refresh Kakao tokens...", setting.getNickname());
            try {
                KakaoMessageService.TokenRefreshResult refreshResult = kakaoMessageService.refreshTokens(setting.getKakaoRefreshToken());
                
                // 데이터베이스의 토큰 값 업데이트
                setting.setKakaoAccessToken(refreshResult.getAccessToken());
                if (refreshResult.getRefreshToken() != null) {
                    setting.setKakaoRefreshToken(refreshResult.getRefreshToken());
                }
                
                // 갱신된 신규 엑세스 토큰으로 재시도 발송!
                log.info("Token refresh successful. Retrying message send for user {}", setting.getNickname());
                success = kakaoMessageService.sendWelstoryMenuToMe(setting.getKakaoAccessToken(), filteredMenu, setting.getCotNo(), setting.getHallNo());
                
                if (success) {
                    log.info("Retried message send successful!");
                } else {
                    log.warn("Retried message send also failed for user {}. Disabling alert.", setting.getNickname());
                    setting.setEnabled(false); // 연속 실패 시 자동 비활성화
                }
                
                welstoryAlertPort.save(setting); // 토큰 및 상태 업데이트 반영
            } catch (Exception refreshEx) {
                log.error("Token refresh failed entirely for user {}. Disabling alert: {}", setting.getNickname(), refreshEx.getMessage());
                setting.setEnabled(false);
                welstoryAlertPort.save(setting);
                return;
            }
        }

        // 4. 발송 완료 마크 (오늘 완료 처리)
        if (success) {
            setting.setLastSentDate(today);
            welstoryAlertPort.save(setting);
            log.info("Scheduled Welstory alert successfully delivered to user {}", setting.getNickname());
        }
    }
}
