package com.amugeonabuster.adapter.out.external;

import com.amugeonabuster.domain.model.WelstoryMenuResult;
import com.amugeonabuster.domain.model.WelstoryMenuResult.CourseMenu;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KakaoApiAdapterTest {

    private final KakaoApiAdapter adapter = new KakaoApiAdapter(
            "test-rest-key",
            "https://lunch.example.com/",
            mock(RestClient.class)
    );

    @Test
    void usesTextTemplateWhenOnlySingleCourseExists() {
        WelstoryMenuResult singleCourseMenu = WelstoryMenuResult.builder()
                .cafeteriaName("수원 본사식당")
                .status("AVAILABLE")
                .courses(List.of(
                        CourseMenu.builder()
                                .courseName("A코스 (아침)")
                                .menuDetails("소고기미역국, 흰밥, 배추김치")
                                .calories(540)
                                .build()
                ))
                .build();

        Map<String, Object> template = adapter.buildTemplate(singleCourseMenu, "COT1", "E11F");

        assertThat(template.get("object_type")).isEqualTo("text");
        assertThat(String.valueOf(template.get("text")))
                .contains("수원 본사식당")
                .contains("A코스 · 540kcal")
                .contains("소고기미역국 · 흰밥");
        @SuppressWarnings("unchecked")
        Map<String, String> link = (Map<String, String>) template.get("link");
        assertThat(link.get("web_url")).startsWith("https://lunch.example.com/?view-menu=true");
    }

    @Test
    void usesListTemplateWhenTwoOrMoreCoursesExist() {
        WelstoryMenuResult multiCourseMenu = WelstoryMenuResult.builder()
                .cafeteriaName("판교 캠퍼스")
                .status("AVAILABLE")
                .courses(List.of(
                        CourseMenu.builder().courseName("한식 (점심)").menuDetails("제육볶음, 된장국").calories(780).build(),
                        CourseMenu.builder().courseName("일품 (점심)").menuDetails("돈코츠라멘, 교자").calories(820).build()
                ))
                .build();

        Map<String, Object> template = adapter.buildTemplate(multiCourseMenu, "COT2", "H02");

        assertThat(template.get("object_type")).isEqualTo("list");
        assertThat(template.get("header_title")).isEqualTo("판교 캠퍼스");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents = (List<Map<String, Object>>) template.get("contents");
        assertThat(contents).hasSize(2);
    }
}
