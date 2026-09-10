package com.amugeonabuster.adapter.out.external;

import com.amugeonabuster.application.port.out.LoadWelstoryMenuPort;
import com.amugeonabuster.domain.model.WelstoryMenuResult;
import com.amugeonabuster.domain.model.WelstoryMenuResult.CourseMenu;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;

@Slf4j
@Component
public class WelplusApiAdapter implements LoadWelstoryMenuPort {

    private final RestTemplate restTemplate;
    private static String cachedToken = null;
    private static long tokenExpiryTime = 0;
    private static final String DEVICE_ID = UUID.randomUUID().toString();

    public WelplusApiAdapter() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); // 3 seconds
        factory.setReadTimeout(5000);    // 5 seconds
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public WelstoryMenuResult loadTodayMenu(String cotNo, String hallNo, String cafeteriaName) {
        return loadMenu(cotNo, hallNo, cafeteriaName, LocalDate.now(ZoneId.of("Asia/Seoul")));
    }

    @Override
    public WelstoryMenuResult loadMenu(String cotNo, String hallNo, String cafeteriaName, LocalDate today) {
        // Heroku 서버는 UTC 기준으로 동작하므로, KST 기준 날짜를 명시적으로 지정
        String dayKorean = today.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
        String dateHeader = String.format("%d월 %d일 (%s)", today.getMonthValue(), today.getDayOfMonth(), dayKorean);
        String yyyyMMdd = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        log.info("Attempting to fetch Welstory menu using direct Welplus API for cafeteria: {}", cafeteriaName);

        try {
            // 웰스토리 API는 토큰 만료 시 401이 아닌 200 OK + 빈 body를 반환하므로,
            // 캐시 유효 시간에 의존하지 않고 매번 새 토큰을 발급받아 사용
            String token = getValidToken(true);
            if (token == null) {
                log.warn("Welstory authentication unavailable.");
                return unavailable(cafeteriaName, dateHeader);
            }

            String restaurantCode = getRestaurantCode(cotNo);
            List<CourseMenu> courses = new ArrayList<>();
            boolean failed = false;

            // 1(아침), 2(점심), 3(저녁), 4(야식) 식단을 순회하며 수집
            for (int mealType = 1; mealType <= 4; mealType++) {
                List<CourseMenu> mealTypeMenus = fetchMenusForMealType(restaurantCode, yyyyMMdd, String.valueOf(mealType));
                if (mealTypeMenus != null) {
                    courses.addAll(mealTypeMenus);
                } else {
                    failed = true;
                }
            }

            if (!courses.isEmpty()) {
                log.info("Successfully loaded {} real-time menus from direct Welplus API!", courses.size());
                return WelstoryMenuResult.builder()
                        .cafeteriaName(cafeteriaName != null ? cafeteriaName : "사내식당")
                        .dateStr(dateHeader)
                        .courses(courses)
                        .status(failed ? "PARTIAL" : "AVAILABLE")
                        .build();
            }

            log.warn("No menus returned from Welplus API.");
            return failed ? unavailable(cafeteriaName, dateHeader) : WelstoryMenuResult.builder()
                    .cafeteriaName(cafeteriaName).dateStr(dateHeader).courses(List.of()).status("EMPTY").build();

        } catch (Exception e) {
            log.warn("Failed to fetch menu from Welplus API ({}).", e.getMessage(), e);
            return unavailable(cafeteriaName, dateHeader);
        }
    }

    private synchronized String getOrFetchToken() {

        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedToken;
        }

        String username = System.getenv("WELSTORY_USERNAME");
        String password = System.getenv("WELSTORY_PASSWORD");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            username = System.getProperty("WELSTORY_USERNAME");
            password = System.getProperty("WELSTORY_PASSWORD");
        }

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalStateException("Welstory credentials (WELSTORY_USERNAME, WELSTORY_PASSWORD) are not set in environment or system properties.");
        }

        log.info("Refreshing Welstory authentication.");
        try {
            String url = "https://welplus.welstory.com/login";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Welplus");
            headers.set("X-Device-Id", DEVICE_ID);
            headers.set("X-Autologin", "N");
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("username", username);
            map.add("password", password);
            map.add("remember-me", "false");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String authHeader = response.getHeaders().getFirst("Authorization");
                if (authHeader != null && !authHeader.isEmpty()) {
                    cachedToken = authHeader;
                    tokenExpiryTime = System.currentTimeMillis() + (2 * 60 * 60 * 1000); // 2 hours
                    log.info("Successfully fetched and cached Welstory Plus JWT token.");
                    return cachedToken;
                }
            }
            log.error("Welstory Plus Login failed. Status: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("Error logging in to Welstory Plus: {}", e.getMessage(), e);
        }
        return null;
    }

    private String getValidToken(boolean forceRefresh) {
        if (forceRefresh) {
            cachedToken = null;
        }
        return getOrFetchToken();
    }

    private String getRestaurantCode(String cotNo) {
        return switch (cotNo == null ? "" : cotNo) {
            case "WEL_DSR" -> "REST000039";
            case "WEL_SUWON" -> "REST000007";
            case "WEL_GIHEUNG" -> {
                String code = System.getenv("WELSTORY_GIHEUNG_RESTAURANT_CODE");
                if (code == null || code.isBlank()) throw new IllegalStateException("기흥 식당 코드가 아직 설정되지 않았습니다.");
                yield code;
            }
            case "WEL_HWASEONG" -> "REST000032";
            case "WEL_SEOCHO" -> "REST000001";
            case "WEL_HQ" -> "REST000050";
            default -> throw new IllegalArgumentException("지원하지 않는 식당 코드입니다.");
        };
    }

    private WelstoryMenuResult unavailable(String cafeteriaName, String date) {
        return WelstoryMenuResult.builder().cafeteriaName(cafeteriaName).dateStr(date)
                .courses(List.of()).status("UNAVAILABLE")
                .message("식당에서 식단 정보를 가져오지 못했어요. 잠시 후 다시 확인해 주세요.").build();
    }

    private List<CourseMenu> fetchMenusForMealType(String restaurantCode, String dateStr, String mealTimeId) {
        String token = getOrFetchToken();
        if (token == null) {
            log.error("Cannot fetch menu for meal type {} because no token is available.", mealTimeId);
            return null;
        }

        try {
            return executeFetchMenus(restaurantCode, dateStr, mealTimeId, token, false);
        } catch (Exception e) {
            log.error("Failed to fetch/parse menu for meal type {}: {}", mealTimeId, e.getMessage(), e);
        }
        return null;
    }

    private List<CourseMenu> executeFetchMenus(String restaurantCode, String dateStr, String mealTimeId, String token, boolean isRetry) throws Exception {
        String url = String.format("https://welplus.welstory.com/api/meal?menuDt=%s&menuMealType=%s&restaurantCode=%s",
                dateStr, mealTimeId, restaurantCode);

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Welplus");
        headers.set("X-Device-Id", DEVICE_ID);
        headers.set("Authorization", token);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response;

        try {
            response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if ((e.getStatusCode() == org.springframework.http.HttpStatus.UNAUTHORIZED || e.getStatusCode() == org.springframework.http.HttpStatus.FORBIDDEN) && !isRetry) {
                log.warn("Token unauthorized (Status: {}). Retrying with forced login refresh.", e.getStatusCode());
                String newToken = getValidToken(true);
                if (newToken != null) {
                    return executeFetchMenus(restaurantCode, dateStr, mealTimeId, newToken, true);
                }
            }
            throw e;
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Failed to fetch menu from Welplus API. Status: {}", response.getStatusCode());
            return null;
        }

        String body = response.getBody();
        // ★ 웰스토리 API 특이 현상: 토큰이 만료되면 401 대신 200 OK와 함께 빈 body(empty string)를 반환함
        if (body == null || body.trim().isEmpty()) {
            if (!isRetry) {
                log.warn("Received empty response body for meal type {}. Token might be expired (200 OK Empty Body). Retrying with forced login refresh.", mealTimeId);
                String newToken = getValidToken(true);
                if (newToken != null) {
                    return executeFetchMenus(restaurantCode, dateStr, mealTimeId, newToken, true);
                }
            }
            log.error("Received empty response body for meal type {} even after token refresh.", mealTimeId);
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(body);

        if (!rootNode.has("data") || rootNode.get("data").isNull()) {
            return null;
        }

        JsonNode dataNode = rootNode.get("data");
        if (!dataNode.has("mealList") || !dataNode.get("mealList").isArray()) {
            return null;
        }

        JsonNode mealList = dataNode.get("mealList");
        List<CourseMenu> menus = new ArrayList<>();
        Set<String> uniqueKeys = new HashSet<>();

        for (JsonNode item : mealList) {
            String courseTxt = item.has("courseTxt") ? item.get("courseTxt").asText().trim() : "";
            String menuName = item.has("menuName") ? item.get("menuName").asText().trim() : "";
            String subMenuTxt = item.has("subMenuTxt") ? item.get("subMenuTxt").asText().trim() : "";
            String menuNameEng = (item.has("menuNameEng") && !item.get("menuNameEng").isNull()) ? item.get("menuNameEng").asText().trim() : "";

            if (menuName.isEmpty() || menuName.contains("주말메뉴안내") || menuName.contains("테이크아웃")) {
                continue;
            }

            String timeLabel = "점심";
            if ("1".equals(mealTimeId)) timeLabel = "아침";
            else if ("3".equals(mealTimeId)) timeLabel = "저녁";
            else if ("4".equals(mealTimeId)) timeLabel = "야식";

            String courseName = courseTxt;
            if (courseName.isEmpty()) {
                courseName = "오늘의 코스";
            }

            if (!menuNameEng.isEmpty()) {
                courseName = String.format("%s (%s) [%s]", courseName, timeLabel, menuNameEng);
            } else {
                courseName = String.format("%s (%s)", courseName, timeLabel);
            }

            String menuDetails = subMenuTxt;
            if (menuDetails.isEmpty()) {
                menuDetails = menuName;
            }

            String uniqueKey = courseName + "||" + menuDetails;
            if (uniqueKeys.contains(uniqueKey)) {
                continue;
            }
            uniqueKeys.add(uniqueKey);

            int calories = 0;
            if (item.has("sumKcal") && !item.get("sumKcal").isNull() && !item.get("sumKcal").asText().trim().isEmpty()) {
                try {
                    calories = Integer.parseInt(item.get("sumKcal").asText().replaceAll("[^0-9]", ""));
                } catch (Exception e) {}
            }
            if (calories == 0 && item.has("kcal") && !item.get("kcal").isNull()) {
                try {
                    calories = Integer.parseInt(item.get("kcal").asText().replaceAll("[^0-9]", ""));
                } catch (Exception e) {}
            }

            String photoCd = item.has("photoCd") ? item.get("photoCd").asText().trim() : "";
            String photoUrl = item.has("photoUrl") ? item.get("photoUrl").asText().trim() : "";
            String imageUrl = "";
            if (!photoCd.isEmpty() && !photoUrl.isEmpty()) {
                imageUrl = photoUrl + photoCd;
                if (imageUrl.startsWith("http://samsungwelstory.com")) {
                    imageUrl = imageUrl.replace("http://", "https://");
                }
            }

            String price = "";

            menus.add(CourseMenu.builder()
                    .courseName(courseName)
                    .menuDetails(menuDetails)
                    .calories(calories)
                    .price(price)
                    .imageUrl(imageUrl)
                    .build());
        }

        return menus;
    }

}
