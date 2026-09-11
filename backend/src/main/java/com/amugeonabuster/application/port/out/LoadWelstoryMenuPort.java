package com.amugeonabuster.application.port.out;

import com.amugeonabuster.domain.model.WelstoryMenuResult;

public interface LoadWelstoryMenuPort {
    WelstoryMenuResult loadTodayMenu(String cotNo, String hallNo, String cafeteriaName);
    WelstoryMenuResult loadMenu(String cotNo, String hallNo, String cafeteriaName, java.time.LocalDate date);

    default WelstoryMenuResult loadMenu(String cotNo, String hallNo, String cafeteriaName,
            java.time.LocalDate date, boolean forceRefresh) {
        return loadMenu(cotNo, hallNo, cafeteriaName, date);
    }
}
