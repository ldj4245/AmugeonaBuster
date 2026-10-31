package com.amugeonabuster.adapter.in.scheduler;

import com.amugeonabuster.application.port.in.CleanupExpiredRoomsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomCleanupScheduler {

    private final CleanupExpiredRoomsUseCase cleanupExpiredRoomsUseCase;

    /**
     * 1시간마다 24시간 이상 지난 만료된 방 데이터 정리
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredRooms() {
        try {
            int deleted = cleanupExpiredRoomsUseCase.cleanupExpiredRooms();
            if (deleted > 0) {
                log.info("Expired rooms cleaned up: count={}", deleted);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup expired rooms: {}", e.getMessage());
        }
    }
}
