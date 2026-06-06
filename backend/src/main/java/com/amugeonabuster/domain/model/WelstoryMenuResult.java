package com.amugeonabuster.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import java.util.List;

@Getter
@Builder
@ToString
public class WelstoryMenuResult {
    private final String cafeteriaName;
    private final String dateStr;
    private final List<CourseMenu> courses;

    @Getter
    @Builder
    @ToString
    public static class CourseMenu {
        private final String courseName;
        private final String menuDetails;
        private final int calories;
        private final String price;
        private final String imageUrl;
    }
}
