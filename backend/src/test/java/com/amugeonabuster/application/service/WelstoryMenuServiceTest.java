package com.amugeonabuster.application.service;

import com.amugeonabuster.adapter.out.external.WelplusApiAdapter;
import com.amugeonabuster.domain.model.WelstoryMenuResult;
import com.amugeonabuster.domain.model.WelstoryMenuResult.CourseMenu;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class WelstoryMenuServiceTest {

    @Test
    public void testGetTodayMenu() {
        WelplusApiAdapter apiAdapter = new WelplusApiAdapter();
        WelstoryMenuService service = new WelstoryMenuService(apiAdapter);
        
        // 환경 변수나 시스템 프로퍼티에서 주입받아 테스트 실행 (하드코딩 방지)
        String username = System.getenv("WELSTORY_USERNAME");
        String password = System.getenv("WELSTORY_PASSWORD");
        if (username == null || username.isEmpty()) {
            username = System.getProperty("WELSTORY_USERNAME");
            password = System.getProperty("WELSTORY_PASSWORD");
        }
        
        org.junit.jupiter.api.Assumptions.assumeTrue(
            username != null && !username.isEmpty() && password != null && !password.isEmpty(),
            "Welstory credentials are not set. Skipping real-time API test."
        );
        
        System.setProperty("WELSTORY_USERNAME", username);
        System.setProperty("WELSTORY_PASSWORD", password);
        
        System.out.println("Fetching real-time menu for DSR...");
        WelstoryMenuResult result = service.getTodayMenu(null, null, "삼성 DSR 타워 웰스토리");
        
        System.out.println("==================================================");
        System.out.println("📢 웰스토리 플러스 API 수집 및 매핑 결과");
        System.out.println("==================================================");
        System.out.println("지점명: " + result.getCafeteriaName());
        System.out.println("날짜: " + result.getDateStr());
        System.out.println("수집된 코스 개수: " + result.getCourses().size());
        
        assertNotNull(result);
        assertEquals("삼성 DSR 타워 웰스토리", result.getCafeteriaName());
        assertFalse(result.getCourses().isEmpty(), "식단 코스 목록이 비어있으면 안 됩니다.");
        
        for (CourseMenu course : result.getCourses()) {
            System.out.println("\n[코스] " + course.getCourseName());
            System.out.println(" - 상세내용: " + course.getMenuDetails());
            System.out.println(" - 칼로리: " + course.getCalories() + " kcal");
            System.out.println(" - 가격: " + course.getPrice());
            System.out.println(" - 이미지: " + course.getImageUrl());
            
            assertNotNull(course.getCourseName());
            assertNotNull(course.getMenuDetails());
            assertNotNull(course.getPrice());
        }
        System.out.println("==================================================");
    }
}
