package com.amugeonabuster.application.port.in;

import com.amugeonabuster.domain.model.WelstoryAlertSettings;
import java.util.Optional;

public interface GetWelstoryAlertQuery {
    Optional<WelstoryAlertSettings> getSettings(String kakaoId);
}
