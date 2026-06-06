package com.amugeonabuster.application.service;

import com.amugeonabuster.application.port.in.GetTodayMenuQuery;
import com.amugeonabuster.application.port.in.GetWelstoryAlertQuery;
import com.amugeonabuster.application.port.in.SaveWelstoryAlertCommand;
import com.amugeonabuster.application.port.out.KakaoApiPort;
import com.amugeonabuster.application.port.out.KakaoApiPort.TokenRefreshResult;
import com.amugeonabuster.application.port.out.WelstoryAlertPort;
import com.amugeonabuster.domain.model.WelstoryAlertSettings;
import com.amugeonabuster.domain.model.WelstoryMenuResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WelstoryAlertService implements GetWelstoryAlertQuery, SaveWelstoryAlertCommand {

    private final WelstoryAlertPort welstoryAlertPort;
    private final GetTodayMenuQuery menuQuery;
    private final KakaoApiPort kakaoApiPort;

    @Override
    public Optional<WelstoryAlertSettings> getSettings(String kakaoId) {
        return welstoryAlertPort.findByKakaoId(kakaoId);
    }

    @Override
    @Transactional
    public WelstoryAlertSettings saveSettings(WelstoryAlertSettings settings) {
        return welstoryAlertPort.save(settings);
    }

    @Override
    @Transactional
    public boolean triggerTestSend(String kakaoId) throws Exception {
        WelstoryAlertSettings setting = welstoryAlertPort.findByKakaoId(kakaoId)
            .orElseThrow(() -> new IllegalArgumentException("설정 정보가 존재하지 않습니다."));

        // 1. 식단 조회
        WelstoryMenuResult menu = menuQuery.getTodayMenu(
            setting.getCotNo(), 
            setting.getHallNo(), 
            setting.getCafeteriaName()
        );

        // 2. 알림 설정 시간에 해당하는 식단만 필터링!
        WelstoryMenuResult filteredMenu = menuQuery.filterMenuByTime(menu, setting.getScheduledTime());

        // 3. 카톡 나에게 보내기 발송
        boolean success = kakaoApiPort.sendWelstoryMenuToMe(setting.getKakaoAccessToken(), filteredMenu, setting.getCotNo(), setting.getHallNo());
        
        if (!success) {
            // 토큰 갱신 시도
            log.info("Test send failed. Attempting to refresh tokens in service...");
            TokenRefreshResult refreshResult = kakaoApiPort.refreshOAuthTokens(setting.getKakaoRefreshToken());
            setting.setKakaoAccessToken(refreshResult.getAccessToken());
            if (refreshResult.getRefreshToken() != null) {
                setting.setKakaoRefreshToken(refreshResult.getRefreshToken());
            }
            welstoryAlertPort.save(setting);

            // 재발송 시도
            success = kakaoApiPort.sendWelstoryMenuToMe(setting.getKakaoAccessToken(), filteredMenu, setting.getCotNo(), setting.getHallNo());
        }
        return success;
    }

    @Override
    @Transactional
    public void resetLastSentDate(String kakaoId) {
        welstoryAlertPort.findByKakaoId(kakaoId).ifPresent(setting -> {
            setting.setLastSentDate(null);
            welstoryAlertPort.save(setting);
            log.info("Successfully reset lastSentDate to null for user: {}", setting.getNickname());
        });
    }

    @Override
    @Transactional
    public void resetAllLastSentDates() {
        welstoryAlertPort.resetAllLastSentDates();
        log.info("Successfully reset lastSentDate to null for all active users.");
    }
}
