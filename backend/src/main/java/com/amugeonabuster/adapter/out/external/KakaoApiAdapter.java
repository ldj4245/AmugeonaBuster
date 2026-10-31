package com.amugeonabuster.adapter.out.external;

import com.amugeonabuster.application.port.out.KakaoApiPort;
import com.amugeonabuster.domain.model.WelstoryMenuResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class KakaoApiAdapter implements KakaoApiPort {

    private final String restKey;
    private final String publicUrl;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public KakaoApiAdapter(
            @Value("${kakao.api.rest-key}") String restKey,
            @Value("${app.public-url:https://amugeona-buster-6eda848df67d.herokuapp.com}") String publicUrl) {
        this(restKey, publicUrl, RestClient.create());
    }

    KakaoApiAdapter(String restKey, String publicUrl, RestClient restClient) {
        this.restKey = restKey;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        this.restClient = restClient;
    }

    @Getter
    @Setter
    @ToString
    public static class KakaoOAuthResponseDto {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;
    }

    @Getter
    @Setter
    @ToString
    public static class KakaoUserMeResponseDto {
        private Long id;
        private Properties properties;

        @Getter
        @Setter
        @ToString
        public static class Properties {
            private String nickname;
        }
    }

    @Getter
    @Setter
    @ToString
    public static class KakaoTokenResponseDto {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        @JsonProperty("refresh_token_expires_in")
        private Integer refreshTokenExpiresIn;
    }

    @Override
    public KakaoOAuthResponse fetchOAuthTokens(String code, String redirectUri) {
        log.info("Exchanging Kakao auth code for tokens. RedirectUri: {}", redirectUri);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", restKey);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        try {
            KakaoOAuthResponseDto dto = restClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(KakaoOAuthResponseDto.class);

            if (dto == null) {
                throw new IllegalStateException("Failed to exchange Kakao OAuth tokens. Empty response.");
            }

            return KakaoOAuthResponse.builder()
                .accessToken(dto.getAccessToken())
                .refreshToken(dto.getRefreshToken())
                .build();
        } catch (Exception e) {
            log.error("Failed to get Kakao OAuth tokens: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public KakaoUserMeResponse fetchUserProfile(String accessToken) {
        log.info("Fetching Kakao user profile...");
        try {
            KakaoUserMeResponseDto dto = restClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserMeResponseDto.class);

            if (dto == null) {
                throw new IllegalStateException("Failed to fetch Kakao user profile. Empty response.");
            }

            String nickname = dto.getProperties() != null ? dto.getProperties().getNickname() : "사용자";

            return KakaoUserMeResponse.builder()
                .id(dto.getId())
                .nickname(nickname)
                .build();
        } catch (Exception e) {
            log.error("Failed to fetch Kakao user profile: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public TokenRefreshResult refreshOAuthTokens(String refreshToken) {
        log.info("Refreshing Kakao tokens using refresh_token...");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", restKey);
        body.add("refresh_token", refreshToken);

        try {
            KakaoTokenResponseDto response = restClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(KakaoTokenResponseDto.class);

            if (response == null || response.getAccessToken() == null) {
                throw new IllegalStateException("Failed to refresh Kakao tokens. Empty response.");
            }

            log.info("Kakao tokens refreshed successfully!");
            return TokenRefreshResult.builder()
                .accessToken(response.getAccessToken())
                .refreshToken(response.getRefreshToken())
                .expiresIn(response.getExpiresIn())
                .build();
        } catch (Exception e) {
            log.error("Failed to refresh Kakao tokens: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean sendWelstoryMenuToMe(String accessToken, WelstoryMenuResult menu, String cotNo, String hallNo) {
        return sendWelstoryMenu(accessToken, menu, cotNo, hallNo) == SendStatus.SENT;
    }

    @Override
    public SendStatus sendWelstoryMenu(String accessToken, WelstoryMenuResult menu, String cotNo, String hallNo) {
        log.info("Sending Welstory menu to KakaoTalk. Cafeteria: {}", menu.getCafeteriaName());

        try {
            Map<String, Object> template = buildTemplate(menu, cotNo, hallNo);
            String templateJson = objectMapper.writeValueAsString(template);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("template_object", templateJson);

            restClient.post()
                .uri("https://kapi.kakao.com/v2/api/talk/memo/default/send")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity();

            log.info("Successfully sent Welstory menu KakaoTalk message.");
            return SendStatus.SENT;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.warn("KakaoTalk memo send returned 401 Unauthorized.");
                return SendStatus.UNAUTHORIZED;
            }
            log.error("KakaoTalk memo send failed with HTTP {}: {}", e.getStatusCode(), e.getMessage());
            return SendStatus.FAILED;
        } catch (Exception e) {
            log.error("Failed to send KakaoTalk memo message: {}", e.getMessage(), e);
            return SendStatus.FAILED;
        }
    }

    Map<String, Object> buildTemplate(WelstoryMenuResult menu, String cotNo, String hallNo) {
        String cafeteriaName = menu.getCafeteriaName() != null ? menu.getCafeteriaName() : "구내식당";
        String encodedLoc = URLEncoder.encode(cafeteriaName, StandardCharsets.UTF_8);
        String viewMenuUrl = publicUrl + "/?view-menu=true&cotNo="
            + cotNo + "&hallNo=" + hallNo + "&name=" + encodedLoc;
        String escapeUrl = publicUrl + "/?escape=true&location=" + encodedLoc;

        Map<String, String> headerLink = new LinkedHashMap<>();
        headerLink.put("web_url", viewMenuUrl);
        headerLink.put("mobile_web_url", viewMenuUrl);

        Map<String, String> escapeLink = new LinkedHashMap<>();
        escapeLink.put("web_url", escapeUrl);
        escapeLink.put("mobile_web_url", escapeUrl);

        List<Map<String, Object>> buttons = new ArrayList<>();
        Map<String, Object> btnEscape = new LinkedHashMap<>();
        btnEscape.put("title", "다른 식당 찾기");
        btnEscape.put("link", escapeLink);
        buttons.add(btnEscape);

        Map<String, Object> btnViewAll = new LinkedHashMap<>();
        btnViewAll.put("title", "전체 메뉴 보기");
        btnViewAll.put("link", headerLink);
        buttons.add(btnViewAll);

        List<WelstoryMenuResult.CourseMenu> courses = menu.getCourses();

        // 카카오 list 템플릿은 contents 항목이 최소 2개 이상(2~3개)일 때만 허용됨.
        // 단일 코스(아침·저녁 단품 등) 또는 빈 코스는 text 템플릿으로 전송해 400 오류를 방지한다.
        if (courses == null || courses.size() < 2) {
            Map<String, Object> textTemplate = new LinkedHashMap<>();
            textTemplate.put("object_type", "text");
            textTemplate.put("text", buildSingleOrEmptyText(cafeteriaName, menu, courses));
            textTemplate.put("link", headerLink);
            textTemplate.put("buttons", buttons);
            return textTemplate;
        }

        Map<String, Object> listTemplate = new LinkedHashMap<>();
        listTemplate.put("object_type", "list");
        listTemplate.put("header_title", cafeteriaName);
        listTemplate.put("header_link", headerLink);

        List<Map<String, Object>> contents = new ArrayList<>();
        int limit = Math.min(courses.size(), 3);
        for (int i = 0; i < limit; i++) {
            WelstoryMenuResult.CourseMenu course = courses.get(i);
            Map<String, Object> item = new LinkedHashMap<>();

            String title = cleanCourseTitle(course.getCourseName());
            if (course.getCalories() > 0) {
                title += " · " + course.getCalories() + "kcal";
            }
            item.put("title", truncate(title, 40));
            item.put("description", compactMenuDetails(course.getMenuDetails()));

            if (course.getImageUrl() != null && !course.getImageUrl().isBlank()) {
                item.put("image_url", course.getImageUrl());
            }
            item.put("link", headerLink);
            contents.add(item);
        }

        listTemplate.put("contents", contents);
        listTemplate.put("buttons", buttons);
        return listTemplate;
    }

    private static String buildSingleOrEmptyText(
            String cafeteriaName,
            WelstoryMenuResult menu,
            List<WelstoryMenuResult.CourseMenu> courses) {
        if (courses == null || courses.isEmpty()) {
            String desc = (menu.getMessage() == null || menu.getMessage().isBlank())
                ? "등록된 메뉴가 없습니다."
                : menu.getMessage();
            return truncate("[" + cafeteriaName + "]\n" + desc, 200);
        }
        WelstoryMenuResult.CourseMenu course = courses.get(0);
        String title = cleanCourseTitle(course.getCourseName());
        if (course.getCalories() > 0) {
            title += " · " + course.getCalories() + "kcal";
        }
        String details = compactMenuDetails(course.getMenuDetails());
        return truncate("[" + cafeteriaName + "]\n" + truncate(title, 60) + "\n" + details, 200);
    }

    static String cleanCourseTitle(String value) {
        if (value == null || value.isBlank()) return "오늘의 메뉴";
        return value.replaceAll("\\s*\\([^)]*\\)", "")
                .replaceAll("\\s*\\[[^]]*\\]", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    static String compactMenuDetails(String value) {
        if (value == null || value.isBlank()) return "메뉴 설명이 없습니다.";
        String[] pieces = value.replace('\n', ',').split(",");
        StringBuilder result = new StringBuilder();
        int shown = 0;
        for (String piece : pieces) {
            String cleaned = piece.replaceAll("\\s*\\([^)]*[A-Za-z][^)]*\\)", "")
                    .replaceAll("\\s{2,}", " ").trim();
            if (cleaned.isBlank()) continue;
            if (result.length() > 0) result.append(" · ");
            result.append(cleaned);
            if (++shown == 2) break;
        }
        return truncate(result.length() == 0 ? "메뉴 설명이 없습니다." : result.toString(), 55);
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(0, max - 1)).trim() + "…";
    }
}
