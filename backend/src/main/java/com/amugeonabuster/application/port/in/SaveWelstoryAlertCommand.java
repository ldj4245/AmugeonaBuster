package com.amugeonabuster.application.port.in;

import com.amugeonabuster.domain.model.WelstoryAlertSettings;

public interface SaveWelstoryAlertCommand {
    WelstoryAlertSettings saveSettings(WelstoryAlertSettings settings);
    boolean triggerTestSend(String kakaoId) throws Exception;
    void resetLastSentDate(String kakaoId);
    void resetAllLastSentDates();
}
