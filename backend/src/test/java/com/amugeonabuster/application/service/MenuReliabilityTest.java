package com.amugeonabuster.application.service;

import com.amugeonabuster.application.port.out.LoadWelstoryMenuPort;
import com.amugeonabuster.domain.model.WelstoryMenuResult;
import com.amugeonabuster.domain.model.WelstoryMenuResult.CourseMenu;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MenuReliabilityTest {
    private final WelstoryMenuService service = new WelstoryMenuService(mock(LoadWelstoryMenuPort.class));

    @Test
    void neverSubstitutesDinnerForMissingLunch() {
        var meal = WelstoryMenuResult.builder().cafeteriaName("식당").dateStr("오늘")
                .courses(List.of(CourseMenu.builder().courseName("A (저녁)").menuDetails("저녁 메뉴").build())).build();
        var filtered = service.filterMenuByTime(meal, "11:30");
        assertThat(filtered.getCourses()).isEmpty();
        assertThat(filtered.getStatus()).isEqualTo("EMPTY");
    }

    @Test
    void partialDataRemainsPartialAfterFiltering() {
        var meal = WelstoryMenuResult.builder().status("PARTIAL").courses(List.of(
                CourseMenu.builder().courseName("A (점심)").menuDetails("점심").build())).build();
        assertThat(service.filterMenuByTime(meal, "11:30").getStatus()).isEqualTo("PARTIAL");
    }

    @Test
    void unavailableDataRemainsUnavailable() {
        var meal = WelstoryMenuResult.builder().status("UNAVAILABLE").courses(List.of()).build();
        assertThat(service.filterMenuByTime(meal, "11:30").getStatus()).isEqualTo("UNAVAILABLE");
    }
}
