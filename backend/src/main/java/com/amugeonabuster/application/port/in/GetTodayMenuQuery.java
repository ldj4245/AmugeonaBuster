package com.amugeonabuster.application.port.in;

import com.amugeonabuster.domain.model.WelstoryMenuResult;

public interface GetTodayMenuQuery {
    WelstoryMenuResult getTodayMenu(String cotNo, String hallNo, String cafeteriaName);
    WelstoryMenuResult filterMenuByTime(WelstoryMenuResult menu, String scheduledTime);
}
