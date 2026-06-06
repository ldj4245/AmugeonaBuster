package com.amugeonabuster.adapter.in.scheduler;

import com.amugeonabuster.application.port.in.SendScheduledAlertsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class WelstoryAlertScheduler {

    private final SendScheduledAlertsCommand sendScheduledAlertsCommand;

    /**
     * 매 분 0초마다 구동되는 배치 스케줄러입니다.
     * 현재 분에 매칭된 유저들을 조회하여 오늘의 웰스토리 식단을 전송합니다.
     */
    @Scheduled(cron = "0 * * * * *")
    public void runScheduledWelstoryAlerts() {
        log.debug("Scheduler trigger fired.");
        sendScheduledAlertsCommand.runScheduledWelstoryAlerts();
    }
}
