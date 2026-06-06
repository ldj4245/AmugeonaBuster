package com.amugeonabuster.application.port.out;

import com.amugeonabuster.domain.model.WelstoryMenuResult;

public interface LoadWelstoryMenuPort {
    WelstoryMenuResult loadTodayMenu(String cotNo, String hallNo, String cafeteriaName);
}
