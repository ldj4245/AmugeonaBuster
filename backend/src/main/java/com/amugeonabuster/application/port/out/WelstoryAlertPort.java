package com.amugeonabuster.application.port.out;

import com.amugeonabuster.domain.model.WelstoryAlertSettings;

import java.util.List;
import java.util.Optional;

public interface WelstoryAlertPort {
    Optional<WelstoryAlertSettings> findByKakaoId(String kakaoId);
    List<WelstoryAlertSettings> findAllByScheduledTimeAndIsEnabled(String scheduledTime);
    WelstoryAlertSettings save(WelstoryAlertSettings setting);
}
