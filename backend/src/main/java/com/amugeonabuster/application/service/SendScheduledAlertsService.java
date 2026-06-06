package com.amugeonabuster.application.service;

import com.amugeonabuster.application.port.in.GetTodayMenuQuery;
import com.amugeonabuster.application.port.in.SendScheduledAlertsCommand;
import com.amugeonabuster.application.port.out.KakaoApiPort;
import com.amugeonabuster.application.port.out.KakaoApiPort.TokenRefreshResult;
import com.amugeonabuster.application.port.out.WelstoryAlertPort;
import com.amugeonabuster.domain.model.WelstoryAlertSettings;
import com.amugeonabuster.domain.model.WelstoryMenuResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SendScheduledAlertsService implements SendScheduledAlertsCommand {

    private final WelstoryAlertPort welstoryAlertPort;
    private final GetTodayMenuQuery menuQuery;
    private final KakaoApiPort kakaoApiPort;

    @Override
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
                // 1. 오늘 날짜에 발송이 예약되어 있는지 확인
                List<String> days = Arrays.asList(setting.getScheduledDays().split(","));
                if (!days.contains(String.valueOf(todayDayOfWeek))) {
                    continue; 
                }

                // 2. 오늘 이미 발송을 완료했는지 체크
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

    private void processAlertSending(WelstoryAlertSettings setting, LocalDate today) {
        log.info("Sending scheduled Welstory alert to user {}", setting.getNickname());

        // 1. 식단 정보 조회
        WelstoryMenuResult menu = menuQuery.getTodayMenu(
            setting.getCotNo(), 
            setting.getHallNo(), 
            setting.getCafeteriaName()
        );

        // 2. 알림 설정 시간에 해당하는 식단만 필터링
        WelstoryMenuResult filteredMenu = menuQuery.filterMenuByTime(menu, setting.getScheduledTime());

        // 3. 카카오톡 메시지 전송 시도
        boolean success = kakaoApiPort.sendWelstoryMenuToMe(setting.getKakaoAccessToken(), filteredMenu, setting.getCotNo(), setting.getHallNo());
        
        if (!success) {
            log.info("Failed to send message to user {}. Attempting to refresh Kakao tokens...", setting.getNickname());
            try {
                TokenRefreshResult refreshResult = kakaoApiPort.refreshOAuthTokens(setting.getKakaoRefreshToken());
                
                setting.setKakaoAccessToken(refreshResult.getAccessToken());
                if (refreshResult.getRefreshToken() != null) {
                    setting.setKakaoRefreshToken(refreshResult.getRefreshToken());
                }
                
                log.info("Token refresh successful. Retrying message send for user {}", setting.getNickname());
                success = kakaoApiPort.sendWelstoryMenuToMe(setting.getKakaoAccessToken(), filteredMenu, setting.getCotNo(), setting.getHallNo());
                
                if (success) {
                    log.info("Retried message send successful!");
                } else {
                    log.warn("Retried message send also failed for user {}. Disabling alert.", setting.getNickname());
                    setting.setEnabled(false);
                }
                
                welstoryAlertPort.save(setting);
            } catch (Exception refreshEx) {
                log.error("Token refresh failed entirely for user {}. Disabling alert: {}", setting.getNickname(), refreshEx.getMessage());
                setting.setEnabled(false);
                welstoryAlertPort.save(setting);
                return;
            }
        }

        // 4. 발송 완료 마크
        if (success) {
            setting.setLastSentDate(today);
            welstoryAlertPort.save(setting);
            log.info("Scheduled Welstory alert successfully delivered to user {}", setting.getNickname());
        }
    }
}
