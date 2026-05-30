package com.amugeonabuster.application.port.in;

import com.amugeonabuster.domain.model.WelstoryAlertSettings;

import java.util.Optional;

public interface WelstoryAlertUseCase {
    Optional<WelstoryAlertSettings> getSettings(String kakaoId);
    WelstoryAlertSettings saveSettings(WelstoryAlertSettings settings);
    boolean triggerTestSend(String kakaoId) throws Exception;
}
