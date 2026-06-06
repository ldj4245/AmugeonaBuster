package com.amugeonabuster.application.service;

import com.amugeonabuster.application.port.in.GetTodayMenuQuery;
import com.amugeonabuster.application.port.out.LoadWelstoryMenuPort;
import com.amugeonabuster.domain.model.WelstoryMenuResult;
import com.amugeonabuster.domain.model.WelstoryMenuResult.CourseMenu;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WelstoryMenuService implements GetTodayMenuQuery {

    private final LoadWelstoryMenuPort loadWelstoryMenuPort;

    @Override
    public WelstoryMenuResult getTodayMenu(String cotNo, String hallNo, String cafeteriaName) {
        return loadWelstoryMenuPort.loadTodayMenu(cotNo, hallNo, cafeteriaName);
    }

    /**
     * 알림 시간대별로 식단을 필터링하여 매칭되는 식단만 반환합니다.
     * 아침: 00:00 ~ 08:59 (hour < 9)
     * 점심: 09:00 ~ 12:59 (hour >= 9 && hour < 13)
     * 저녁/야식: 13:00 ~ 23:59 (hour >= 13)
     */
    @Override
    public WelstoryMenuResult filterMenuByTime(WelstoryMenuResult menu, String scheduledTime) {
        if (menu == null || menu.getCourses() == null || menu.getCourses().isEmpty()) {
            return menu;
        }

        String targetLabel = "점심"; // 기본값
        try {
            if (scheduledTime != null && scheduledTime.contains(":")) {
                String[] parts = scheduledTime.split(":");
                int hour = Integer.parseInt(parts[0]);
                if (hour < 9) {
                    targetLabel = "아침";
                } else if (hour >= 13) {
                    targetLabel = "저녁";
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse scheduledTime: {}, defaulting to 점심", scheduledTime, e);
        }

        log.info("Filtering courses for cafeteria: {} matching time label: '{}' (Scheduled: {})", 
            menu.getCafeteriaName(), targetLabel, scheduledTime);

        List<CourseMenu> filteredCourses = new ArrayList<>();
        String searchSuffix = "(" + targetLabel + ")";
        for (CourseMenu course : menu.getCourses()) {
            if (course.getCourseName() != null && course.getCourseName().contains(searchSuffix)) {
                filteredCourses.add(course);
            }
        }

        if (filteredCourses.isEmpty()) {
            log.warn("No courses matched the filter '{}'. Returning original courses as fallback.", targetLabel);
            return menu;
        }

        return WelstoryMenuResult.builder()
            .cafeteriaName(menu.getCafeteriaName())
            .dateStr(menu.getDateStr())
            .courses(filteredCourses)
            .build();
    }
}
