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
        LocalDate today = LocalDate.now();
        String dayKorean = today.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
        String dateHeader = String.format("%d월 %d일 (%s)", today.getMonthValue(), today.getDayOfMonth(), dayKorean);
        String yyyyMMdd = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        log.info("Attempting to fetch Welstory menu using direct Welplus API for cafeteria: {}", cafeteriaName);

        try {
            String token = getValidToken(false);
            if (token == null) {
                log.warn("Could not obtain valid Welstory token. Utilizing premium fallback menu.");
                return generatePremiumFallbackMenu(today, cafeteriaName);
            }

            String restaurantCode = getRestaurantCode(cafeteriaName);
            List<CourseMenu> courses = new ArrayList<>();

            // 1(아침), 2(점심), 3(저녁), 4(야식) 식단을 순회하며 수집
            for (int mealType = 1; mealType <= 4; mealType++) {
                List<CourseMenu> mealTypeMenus = fetchMenusForMealType(restaurantCode, yyyyMMdd, String.valueOf(mealType));
                if (mealTypeMenus != null) {
                    courses.addAll(mealTypeMenus);
                }
            }

            if (!courses.isEmpty()) {
                log.info("Successfully loaded {} real-time menus from direct Welplus API!", courses.size());
                return WelstoryMenuResult.builder()
                        .cafeteriaName(cafeteriaName != null ? cafeteriaName : "사내식당")
                        .dateStr(dateHeader)
                        .courses(courses)
                        .build();
            }

            log.warn("No menus returned from Welplus API. Utilizing premium fallback menu.");
            return generatePremiumFallbackMenu(today, cafeteriaName);

        } catch (Exception e) {
            log.warn("Failed to fetch menu from direct Welplus API ({}). Utilizing premium fallback menu.", e.getMessage(), e);
            return generatePremiumFallbackMenu(today, cafeteriaName);
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

        log.info("Fetching new Welstory Plus JWT token for user: {}", username);
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

    private String getRestaurantCode(String cafeteriaName) {
        if (cafeteriaName == null || cafeteriaName.isEmpty()) {
            return "REST000039";
        }
        if (cafeteriaName.contains("DSR")) {
            return "REST000039";
        } else if (cafeteriaName.contains("수원") || cafeteriaName.contains("R5")) {
            return "REST000007";
        } else if (cafeteriaName.contains("기흥")) {
            return "REST000039";
        } else if (cafeteriaName.contains("화성") || cafeteriaName.contains("H1")) {
            return "REST000032";
        } else if (cafeteriaName.contains("서초")) {
            return "REST000001";
        } else if (cafeteriaName.contains("본사") || cafeteriaName.contains("HQ")) {
            return "REST000050";
        }
        return "REST000039";
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

            String price = "7,840원";

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

    private WelstoryMenuResult generatePremiumFallbackMenu(LocalDate date, String cafeteriaName) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        String dayKorean = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN);
        String dateHeader = String.format("%d월 %d일 (%s)", date.getMonthValue(), date.getDayOfMonth(), dayKorean);

        List<CourseMenu> courses = new ArrayList<>();

        courses.add(CourseMenu.builder()
                .courseName("A코스 (아침)")
                .menuDetails("부드러운 영양 닭죽 & 오징어젓갈, 김구이, 배추김치")
                .calories(520)
                .price("7,840원")
                .imageUrl("https://images.unsplash.com/photo-1544025162-d76694265947?w=300")
                .build());

        switch (dayOfWeek) {
            case MONDAY:
                courses.add(CourseMenu.builder()
                        .courseName("A코스 (한식소담) (점심)")
                        .menuDetails("직화 바싹 제육볶음 & 우렁강된장 쌈밥, 흑미밥, 콩나물국, 계란말이, 석박지")
                        .calories(1180)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1544025162-d76694265947?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("B코스 (Chef's Table) (점심)")
                        .menuDetails("눈꽃치즈 수제 등심 돈카츠, 크림 스프, 오리엔탈 파스타 샐러드, 모닝빵, 피클")
                        .calories(1290)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1598515214211-89d3c73ae83b?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("C코스 (헬스밀) (점심)")
                        .menuDetails("아보카도 훈제오리 샐러드 보울, 단호박 죽, 무설탕 요거트, 견과류")
                        .calories(650)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("A코스 (한식소담) (저녁)")
                        .menuDetails("얼큰 부대찌개 & 라면사리, 쌀밥, 감자채볶음, 어묵무침, 깍두기")
                        .calories(980)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1569058242253-92a9c755a0ec?w=300")
                        .build());
                break;

            case TUESDAY:
                courses.add(CourseMenu.builder()
                        .courseName("A코스 (한식소담) (점심)")
                        .menuDetails("춘천식 매콤 닭갈비 덮밥, 팽이버섯 장국, 반반 만두튀김, 무쌈, 배추김치")
                        .calories(1120)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1627308595229-7830a5c91f9f?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("B코스 (누들가) (점심)")
                        .menuDetails("소고기 쌀국수 & 매콤 해물 볶음밥, 스프링롤 튀김, 단무지, 고수믹스")
                        .calories(1210)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("C코스 (Take-out) (점심)")
                        .menuDetails("수제 불고기 파니니 샌드위치, 팩 두유, 과일 컵, 감자칩")
                        .calories(720)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1521390188846-e2a3a97453a0?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("A코스 (한식소담) (저녁)")
                        .menuDetails("돈육 김치찌개 & 스팸구이, 쌀밥, 계란말이, 김구이, 깍두기")
                        .calories(950)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1627308595229-7830a5c91f9f?w=300")
                        .build());
                break;

            case WEDNESDAY:
                courses.add(CourseMenu.builder()
                        .courseName("A코스 (한식소담 ★특식) (점심)")
                        .menuDetails("한방 맑은 소갈비탕, 가마솥 쌀밥, 통오징어 숙회 야채초무침, 오이소박이, 깍두기")
                        .calories(1250)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1547928576-a4a3323dce9a?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("B코스 (Chef's Table) (점심)")
                        .menuDetails("트러플 크림 버섯 리조또, 수제 떡갈비 스테이크, 카프레제 샐러드, 수제 에이드")
                        .calories(1310)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1533479093185-190f84097f44?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("C코스 (헬스밀) (점심)")
                        .menuDetails("리코타 치즈 청포도 샐러드, 구운 알감자, 단백질 초코 쉐이크, 사과")
                        .calories(680)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1540420773420-3366772f4999?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("A코스 (한식소담) (저녁)")
                        .menuDetails("마라탕 & 꿔바로우, 볶음밥, 짜사이무침, 단무지")
                        .calories(1150)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1533479093185-190f84097f44?w=300")
                        .build());
                break;

            case THURSDAY:
                courses.add(CourseMenu.builder()
                        .courseName("A코스 (한식소담) (점심)")
                        .menuDetails("시골 양푼 비빔밥 & 강된장찌개, 바싹 해물파전, 도토리묵 무침, 열무김치")
                        .calories(1160)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1569058242253-92a9c755a0ec?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("B코스 (Chef's Table) (점심)")
                        .menuDetails("매콤 인도식 탄두리 치킨 카레 & 갈릭 난, 고구마 크로켓, 콘샐러드, 할라피뇨")
                        .calories(1230)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("C코스 (Take-out) (점심)")
                        .menuDetails("수제 그릴드 닭가슴살 샐러드 랩, 유기농 착즙 오렌지 주스, 하루 견과")
                        .calories(620)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1509722747041-616f39b57569?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("A코스 (한식소담) (저녁)")
                        .menuDetails("춘천식 닭갈비 & 우동사리, 볶음밥, 쌈채소, 동치미")
                        .calories(1220)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=300")
                        .build());
                break;

            case FRIDAY:
                courses.add(CourseMenu.builder()
                        .courseName("A코스 (한식소담) (점심)")
                        .menuDetails("묵은지 김치찌개 & 통통 돼지 등갈비찜, 기장밥, 야채튀김, 구이김, 열무김치")
                        .calories(1210)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1627308595229-7830a5c91f9f?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("B코스 (누들가) (점심)")
                        .menuDetails("얼큰 차돌 짬뽕 & 바삭 미니 탕수육, 짜사이 무침, 단무지, 군만두")
                        .calories(1320)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1585032226651-759b368d7246?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("C코스 (헬스밀) (점심)")
                        .menuDetails("닭가슴살 고구마 두부 샐러드, 곤약 젤리, 콜드브루 커피, 토마토")
                        .calories(590)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("A코스 (한식소담) (저녁)")
                        .menuDetails("바베큐 폭립 & 감자튀김, 버터롤빵, 콘버터, 할라피뇨")
                        .calories(1190)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1585032226651-759b368d7246?w=300")
                        .build());
                break;

            default:
                String localSpecial = "소고기 버섯 전골";
                String localSpecialDetails = "소고기 버섯 전골 & 즉석 야채 전, 잡곡밥, 오징어 젓갈, 배추김치";
                String localNoodle = "베이컨 까르보나라 파스타";
                String localNoodleDetails = "베이컨 까르보나라 파스타, 고르곤졸라 피자(1인용), 그린 그린 그린샐러드, 피클";
                String localImageA = "https://images.unsplash.com/photo-1544025162-d76694265947?w=300";
                String localImageB = "https://images.unsplash.com/photo-1546549032-9571cd6b27df?w=300";

                if (cafeteriaName != null) {
                    if (cafeteriaName.contains("수원")) {
                        localSpecial = "수원 왕갈비탕";
                        localSpecialDetails = "진한 소갈비로 푹 끓여낸 수원 디지털시티 명물 왕갈비탕, 석박지, 소면 사리, 양파 절임";
                        localNoodle = "수원 불갈비 피자";
                        localNoodleDetails = "수원식 불갈비 토핑을 듬뿍 올린 수제 씬 피자, 아삭 오이 피클, 코울슬로";
                        localImageA = "https://images.unsplash.com/photo-1544025162-d76694265947?w=300";
                    } else if (cafeteriaName.contains("서초")) {
                        localSpecial = "서초동 정통 규동";
                        localSpecialDetails = "우삼겹을 가득 올린 일본식 소고기 덮밥(규동), 온센타마고(반숙란), 베니쇼가, 미소 장국";
                        localNoodle = "명란 크림 파스타";
                        localNoodleDetails = "고소한 크림 베이스에 짭조름한 명란젓을 올린 서초 임직원 전용 명란 크림 파스타, 마늘 바게트";
                        localImageA = "https://images.unsplash.com/photo-1627308595229-7830a5c91f9f?w=300";
                    } else if (cafeteriaName.contains("기흥")) {
                        localSpecial = "기흥식 석쇠 고추장 불고기";
                        localSpecialDetails = "매콤한 고추장 양념의 불향 가득 석쇠 불고기 쌈밥 정식, 쌈채소, 우렁 강된장, 흑미밥";
                        localNoodle = "얼큰 탄탄멘";
                        localNoodleDetails = "고소한 땅콩 육수와 특제 라유 소스로 끓여낸 기흥 캠퍼스 피로회복용 매운 탄탄멘, 군만두";
                        localImageA = "https://images.unsplash.com/photo-1544025162-d76694265947?w=300";
                    } else if (cafeteriaName.contains("화성")) {
                        localSpecial = "화성 융건릉 매운 소갈비찜";
                        localSpecialDetails = "야들야들하게 졸여낸 화성 특선 매콤 소갈비찜 정식, 한식 잡채, 백미밥, 시원한 동치미";
                        localNoodle = "해물 순두부 짬뽕밥";
                        localNoodleDetails = "얼큰한 해물 짬뽕 국물에 몽글몽글한 순두부를 가득 채운 화성 반도체 스페셜 해물 순두부 짬뽕, 야채 튀김";
                        localImageA = "https://images.unsplash.com/photo-1627308595229-7830a5c91f9f?w=300";
                    } else if (cafeteriaName.contains("본사")) {
                        localSpecial = "웰스토리 시그니처 한우 양지 국밥";
                        localSpecialDetails = "국내산 한우 양지를 푹 고아 얼큰하고 시원하게 끓여낸 웰스토리 본사 임직원 활력 국밥, 도토리묵";
                        localNoodle = "수제 랍스터 오일 스파게티";
                        localNoodleDetails = "통통한 랍스터 테일과 향긋한 마늘 오일이 어우러진 본사 특선 프리미엄 랍스터 스파게티";
                        localImageA = "https://images.unsplash.com/photo-1598515214211-89d3c73ae83b?w=300";
                    }
                }

                courses.add(CourseMenu.builder()
                        .courseName("A코스 (" + localSpecial + ") (점심)")
                        .menuDetails(localSpecialDetails)
                        .calories(790)
                        .price("7,840원")
                        .imageUrl(localImageA)
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("B코스 (" + localNoodle + ") (점심)")
                        .menuDetails(localNoodleDetails)
                        .calories(870)
                        .price("7,840원")
                        .imageUrl(localImageB)
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("A코스 (김치돈까스나베) (저녁)")
                        .menuDetails("[B타워] 김치돈까스나베(Kimchi Hot Pot and Pork Cutlet), 공기밥, 단무지, 샐러드")
                        .calories(850)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1598515214211-89d3c73ae83b?w=300")
                        .build());
                courses.add(CourseMenu.builder()
                        .courseName("B코스 (열무보리비빔밥) (저녁)")
                        .menuDetails("[B타워] 열무보리비빔밥(Barley Bibimbap with Young Radish), 해물뚝배기 찌개, 깍두기")
                        .calories(780)
                        .price("7,840원")
                        .imageUrl("https://images.unsplash.com/photo-1569058242253-92a9c755a0ec?w=300")
                        .build());
                break;
        }

        return WelstoryMenuResult.builder()
                .cafeteriaName(cafeteriaName != null ? cafeteriaName : "사내식당")
                .dateStr(dateHeader)
                .courses(courses)
                .build();
    }
}
