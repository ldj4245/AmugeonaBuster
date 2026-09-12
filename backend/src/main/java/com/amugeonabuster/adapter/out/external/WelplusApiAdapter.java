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
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class WelplusApiAdapter implements LoadWelstoryMenuPort {

    private final RestTemplate restTemplate;
    private volatile String cachedToken;
    private volatile long tokenExpiryTime;
    private static final String DEVICE_ID = UUID.randomUUID().toString();
    private static final long MENU_CACHE_TTL_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final long AVAILABLE_STALE_TTL_MILLIS = Duration.ofHours(6).toMillis();
    private static final long EMPTY_CACHE_TTL_MILLIS = Duration.ofMinutes(2).toMillis();
    private static final long UNAVAILABLE_CACHE_TTL_MILLIS = Duration.ofSeconds(30).toMillis();
    private static final long FALLBACK_TOKEN_TTL_MILLIS = Duration.ofMinutes(8).toMillis();
    private static final long TOKEN_EXPIRY_SAFETY_MILLIS = Duration.ofSeconds(30).toMillis();
    private final Map<MenuCacheKey, CachedMenu> menuCache = new ConcurrentHashMap<>();
    private final Map<MenuCacheKey, CompletableFuture<WelstoryMenuResult>> inFlight = new ConcurrentHashMap<>();
    private final ExecutorService requestExecutor;
    private final ExecutorService menuExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WelplusApiAdapter() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); // 3 seconds
        factory.setReadTimeout(5000);    // 5 seconds
        this.restTemplate = new RestTemplate(factory);
        AtomicInteger threadNumber = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "welstory-menu-" + threadNumber.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        this.requestExecutor = Executors.newFixedThreadPool(8, threadFactory);
        this.menuExecutor = Executors.newFixedThreadPool(4, threadFactory);
    }

    @PreDestroy
    void stopMenuExecutor() {
        requestExecutor.shutdownNow();
        menuExecutor.shutdownNow();
    }

    @Override
    public WelstoryMenuResult loadTodayMenu(String cotNo, String hallNo, String cafeteriaName) {
        return loadMenu(cotNo, hallNo, cafeteriaName, LocalDate.now(ZoneId.of("Asia/Seoul")));
    }

    @Override
    public WelstoryMenuResult loadMenu(String cotNo, String hallNo, String cafeteriaName, LocalDate today) {
        return loadMenu(cotNo, hallNo, cafeteriaName, today, false);
    }

    @Override
    public WelstoryMenuResult loadMenu(String cotNo, String hallNo, String cafeteriaName, LocalDate today,
            boolean forceRefresh) {
        MenuCacheKey cacheKey = new MenuCacheKey(cotNo, hallNo, today);
        if (forceRefresh) {
            log.debug("Bypassing Welstory menu cache for {} {}", cotNo, today);
        } else {
            CachedMenu cached = menuCache.get(cacheKey);
            if (cached != null && cached.isFresh()) {
                log.debug("Returning cached Welstory menu for {} {}", cotNo, today);
                return cached.result();
            }
            if (cached != null && !cached.canFallback()) {
                menuCache.remove(cacheKey, cached);
            }
        }

        CompletableFuture<WelstoryMenuResult> request = inFlight.computeIfAbsent(cacheKey, key -> {
            CompletableFuture<WelstoryMenuResult> created = CompletableFuture.supplyAsync(
                    () -> fetchMenu(cotNo, hallNo, cafeteriaName, today), requestExecutor);
            created.whenComplete((result, error) -> {
                inFlight.remove(key, created);
                if (error == null && result != null) {
                    long now = System.currentTimeMillis();
                    long cacheTtl = cacheTtlFor(result);
                    CachedMenu updated = new CachedMenu(result, now + cacheTtl,
                            isAvailable(result) ? now + AVAILABLE_STALE_TTL_MILLIS : now + cacheTtl);
                    menuCache.compute(key, (ignored, current) ->
                            "UNAVAILABLE".equals(result.getStatus()) && current != null && current.canFallback()
                                    ? current : updated);
                }
            });
            return created;
        });

        try {
            WelstoryMenuResult result = request.join();
            if ("UNAVAILABLE".equals(result.getStatus())) {
                CachedMenu fallback = menuCache.get(cacheKey);
                if (fallback != null && fallback.canFallback()) {
                    log.warn("Serving the most recent Welstory menu after an upstream failure for {} {}", cotNo, today);
                    return fallback.result();
                }
            }
            return result;
        } catch (CompletionException e) {
            log.warn("Welstory menu request failed ({}).", e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
            String dayKorean = today.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
            return unavailable(cafeteriaName,
                    String.format("%d월 %d일 (%s)", today.getMonthValue(), today.getDayOfMonth(), dayKorean));
        }
    }

    private long cacheTtlFor(WelstoryMenuResult result) {
        if ("UNAVAILABLE".equals(result.getStatus())) return UNAVAILABLE_CACHE_TTL_MILLIS;
        if ("EMPTY".equals(result.getStatus()) || "PARTIAL".equals(result.getStatus())) return EMPTY_CACHE_TTL_MILLIS;
        return MENU_CACHE_TTL_MILLIS;
    }

    private boolean isAvailable(WelstoryMenuResult result) {
        return "AVAILABLE".equals(result.getStatus())
                && result.getCourses() != null && !result.getCourses().isEmpty();
    }

    private WelstoryMenuResult fetchMenu(String cotNo, String hallNo, String cafeteriaName, LocalDate today) {
        // Heroku 서버는 UTC 기준으로 동작하므로, KST 기준 날짜를 명시적으로 지정
        String dayKorean = today.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
        String dateHeader = String.format("%d월 %d일 (%s)", today.getMonthValue(), today.getDayOfMonth(), dayKorean);
        String yyyyMMdd = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        log.info("Attempting to fetch Welstory menu using direct Welplus API for cafeteria: {}", cafeteriaName);

        try {
            String token = getOrFetchToken();
            if (token == null) {
                log.warn("Welstory authentication unavailable.");
                return unavailable(cafeteriaName, dateHeader);
            }

            String restaurantCode = getRestaurantCode(cotNo);
            List<CourseMenu> courses = new ArrayList<>();
            boolean failed = false;

            // 1(아침), 2(점심), 3(저녁), 4(야식) 식단을 동시에 수집
            List<CompletableFuture<List<CourseMenu>>> requests = new ArrayList<>(4);
            for (int mealType = 1; mealType <= 4; mealType++) {
                String mealTypeId = String.valueOf(mealType);
                requests.add(CompletableFuture.supplyAsync(
                        () -> fetchMenusForMealType(restaurantCode, yyyyMMdd, mealTypeId), menuExecutor));
            }
            for (CompletableFuture<List<CourseMenu>> request : requests) {
                List<CourseMenu> mealTypeMenus = request.join();
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

    private record MenuCacheKey(String cotNo, String hallNo, LocalDate date) {}

    private record CachedMenu(WelstoryMenuResult result, long expiresAt, long staleUntil) {
        private boolean isFresh() {
            return expiresAt > System.currentTimeMillis();
        }

        private boolean canFallback() {
            return staleUntil > System.currentTimeMillis();
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
                    tokenExpiryTime = resolveTokenExpiry(authHeader);
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

    long resolveTokenExpiry(String authorizationHeader) {
        long fallback = System.currentTimeMillis() + FALLBACK_TOKEN_TTL_MILLIS;
        try {
            String jwt = authorizationHeader.replaceFirst("(?i)^Bearer\\s+", "");
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) return fallback;
            JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            long expiresAt = claims.path("exp").asLong(0) * 1000;
            if (expiresAt <= System.currentTimeMillis()) return fallback;
            return expiresAt - TOKEN_EXPIRY_SAFETY_MILLIS;
        } catch (RuntimeException | java.io.IOException e) {
            log.debug("Could not read Welstory token expiry. Using the fallback TTL.");
            return fallback;
        }
    }

    private synchronized String refreshToken(String failedToken) {
        // 여러 병렬 요청이 동시에 만료를 감지해도 로그인은 한 번만 수행합니다.
        if (cachedToken != null && !cachedToken.equals(failedToken)
                && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedToken;
        }
        cachedToken = null;
        tokenExpiryTime = 0;
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
                String newToken = refreshToken(token);
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
                String newToken = refreshToken(token);
                if (newToken != null) {
                    return executeFetchMenus(restaurantCode, dateStr, mealTimeId, newToken, true);
                }
            }
            log.error("Received empty response body for meal type {} even after token refresh.", mealTimeId);
            return null;
        }

        JsonNode rootNode = objectMapper.readTree(body);

        if (!rootNode.has("data") || rootNode.get("data").isNull()) {
            return retryAfterInvalidSession(restaurantCode, dateStr, mealTimeId, token, isRetry,
                    "data가 없는 응답");
        }

        JsonNode dataNode = rootNode.get("data");
        if (!dataNode.has("mealList") || !dataNode.get("mealList").isArray()) {
            return retryAfterInvalidSession(restaurantCode, dateStr, mealTimeId, token, isRetry,
                    "mealList가 없는 응답");
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

    private List<CourseMenu> retryAfterInvalidSession(String restaurantCode, String dateStr, String mealTimeId,
            String token, boolean isRetry, String reason) throws Exception {
        if (!isRetry) {
            log.warn("Welstory returned {} for meal type {}. Refreshing authentication once.", reason, mealTimeId);
            String newToken = refreshToken(token);
            if (newToken != null) {
                return executeFetchMenus(restaurantCode, dateStr, mealTimeId, newToken, true);
            }
        }
        log.error("Welstory returned {} for meal type {} after authentication refresh.", reason, mealTimeId);
        return null;
    }

}
