package com.amugeonabuster.application.service;

import com.amugeonabuster.domain.model.WelstoryAlertSettings;
import com.amugeonabuster.application.port.in.WelstoryAlertUseCase;
import com.amugeonabuster.application.port.out.WelstoryAlertPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WelstoryAlertService implements WelstoryAlertUseCase {

    private final WelstoryAlertPort welstoryAlertPort;
    private final WelstoryMenuService menuService;
    private final KakaoMessageService kakaoMessageService;

    @Override
    public Optional<WelstoryAlertSettings> getSettings(String kakaoId) {
        return welstoryAlertPort.findByKakaoId(kakaoId);
    }

    @Override
    public WelstoryAlertSettings saveSettings(WelstoryAlertSettings settings) {
        return welstoryAlertPort.save(settings);
    }

    @Override
    public boolean triggerTestSend(String kakaoId) throws Exception {
        WelstoryAlertSettings setting = welstoryAlertPort.findByKakaoId(kakaoId)
            .orElseThrow(() -> new IllegalArgumentException("설정 정보가 존재하지 않습니다."));

        // 1. 식단 조회
        WelstoryMenuService.WelstoryMenuResult menu = menuService.getTodayMenu(
            setting.getCotNo(), 
            setting.getHallNo(), 
            setting.getCafeteriaName()
        );

        // 2. 카톡 나에게 보내기 발송
        boolean success = kakaoMessageService.sendWelstoryMenuToMe(setting.getKakaoAccessToken(), menu, setting.getCotNo(), setting.getHallNo());
        
        if (!success) {
            // 토큰 갱신 시도
            log.info("Test send failed. Attempting to refresh tokens in service...");
            KakaoMessageService.TokenRefreshResult refreshResult = kakaoMessageService.refreshTokens(setting.getKakaoRefreshToken());
            setting.setKakaoAccessToken(refreshResult.getAccessToken());
            if (refreshResult.getRefreshToken() != null) {
                setting.setKakaoRefreshToken(refreshResult.getRefreshToken());
            }
            welstoryAlertPort.save(setting);

            // 재발송 시도
            success = kakaoMessageService.sendWelstoryMenuToMe(setting.getKakaoAccessToken(), menu, setting.getCotNo(), setting.getHallNo());
        }
        return success;
    }

    @Override
    public void resetLastSentDate(String kakaoId) {
        welstoryAlertPort.findByKakaoId(kakaoId).ifPresent(setting -> {
            setting.setLastSentDate(null);
            welstoryAlertPort.save(setting);
            log.info("Successfully reset lastSentDate to null for user: {}", setting.getNickname());
        });
    }

    @Override
    public void resetAllLastSentDates() {
        welstoryAlertPort.resetAllLastSentDates();
        log.info("Successfully reset lastSentDate to null for all active users.");
    }
}
