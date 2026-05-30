package com.amugeonabuster.application.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KakaoMessageService {

    private final String restKey;
    private final RestClient restClient;

    public KakaoMessageService(@Value("${kakao.api.rest-key}") String restKey) {
        this.restKey = restKey;
        this.restClient = RestClient.create();
    }

    @Getter
    @Builder
    @ToString
    public static class TokenRefreshResult {
        private final String accessToken;
        private final String refreshToken; // Optional: Only present if Kakao refreshes the Refresh Token
        private final Integer expiresIn;
    }

    /**
     * 카카오 리프레시 토큰을 이용해 엑세스 토큰을 갱신합니다.
     */
    public TokenRefreshResult refreshTokens(String refreshToken) throws Exception {
        log.info("Refreshing Kakao tokens using refresh_token...");
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", restKey);
        body.add("refresh_token", refreshToken);

        try {
            KakaoTokenResponse response = restClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(KakaoTokenResponse.class);

            if (response == null || response.getAccessToken() == null) {
                throw new IllegalStateException("Failed to refresh Kakao tokens. Empty response.");
            }

            log.info("Kakao tokens refreshed successfully!");
            return TokenRefreshResult.builder()
                .accessToken(response.getAccessToken())
                .refreshToken(response.getRefreshToken()) // Kakao might return null if not updated
                .expiresIn(response.getExpiresIn())
                .build();
        } catch (Exception e) {
            log.error("Failed to refresh Kakao tokens: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Getter
    @Setter
    @ToString
    public static class KakaoOAuthResponse {
        @JsonProperty("access_token")
        private String accessToken;
        
        @JsonProperty("refresh_token")
        private String refreshToken;
    }

    @Getter
    @Setter
    @ToString
    public static class KakaoUserMeResponse {
        private Long id;
        private Properties properties;

        @Getter
        @Setter
        @ToString
        public static class Properties {
            private String nickname;
        }
    }

    /**
     * 카카오 인증 코드를 이용해 엑세스 및 리프레시 토큰을 최초로 발급받습니다.
     */
    public KakaoOAuthResponse getOAuthTokens(String code, String redirectUri) {
        log.info("Exchanging Kakao auth code for tokens. RedirectUri: {}", redirectUri);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", restKey);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        try {
            return restClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(KakaoOAuthResponse.class);
        } catch (Exception e) {
            log.error("Failed to get Kakao OAuth tokens: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 카카오 엑세스 토큰을 이용해 사용자 프로필(ID, 닉네임)을 가져옵니다.
     */
    public KakaoUserMeResponse getUserMe(String accessToken) {
        log.info("Fetching Kakao user profile...");
        try {
            return restClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserMeResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch Kakao user profile: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 웰스토리 식단 결과를 '카카오톡 나에게 보내기' API로 발송합니다.
     * 모바일 화면에서의 글자 수 잘림을 완벽히 방지하기 위해 '리스트(List)형 템플릿'을 적용합니다.
     */
    public boolean sendWelstoryMenuToMe(String accessToken, WelstoryMenuService.WelstoryMenuResult menu) {
        log.info("Sending Welstory menu to user KakaoTalk using LIST template. Cafeteria: {}", menu.getCafeteriaName());
        
        try {
            Map<String, Object> template = new HashMap<>();
            template.put("object_type", "list");
            
            // 1. 헤더 타이틀 설정
            template.put("header_title", "🍱 " + menu.getCafeteriaName());
            
            Map<String, String> headerLink = new HashMap<>();
            headerLink.put("web_url", "https://amugeona-buster-6eda848df67d.herokuapp.com");
            headerLink.put("mobile_web_url", "https://amugeona-buster-6eda848df67d.herokuapp.com");
            template.put("header_link", headerLink);

            // 2. 리스트에 들어갈 아이템 콘텐츠 구성 (카카오 규격상 최대 3개 행 노출 지원)
            List<Map<String, Object>> contents = new ArrayList<>();
            List<WelstoryMenuService.CourseMenu> courses = menu.getCourses();
            
            if (courses.isEmpty()) {
                Map<String, Object> item = new HashMap<>();
                item.put("title", "오늘의 구내식당 식단");
                item.put("description", "등록된 식단 정보가 없습니다.");
                item.put("image_url", "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200");
                item.put("link", headerLink);
                contents.add(item);
            } else {
                int limit = Math.min(courses.size(), 3);
                for (int i = 0; i < limit; i++) {
                    WelstoryMenuService.CourseMenu course = courses.get(i);
                    Map<String, Object> item = new HashMap<>();
                    
                    // 제목: 코스 구분 + 칼로리 (예: ⭐ A코스 (790kcal))
                    item.put("title", "⭐ " + course.getCourseName() + " (" + course.getCalories() + "kcal)");
                    
                    // 본문: 반찬 리스트를 긴 말줄임표 없이 가로로 깔끔하게 요약 쉼표 표시
                    String details = course.getMenuDetails().replace("\n", ", ").trim();
                    if (details.endsWith(",")) {
                        details = details.substring(0, details.length() - 1);
                    }
                    if (details.length() > 50) {
                        details = details.substring(0, 47) + "...";
                    }
                    item.put("description", details);
                    
                    // 음식 개별 썸네일 (없으면 기본 요리 이미지 사용)
                    String img = course.getImageUrl();
                    if (img == null || img.isEmpty()) {
                        img = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200";
                    }
                    item.put("image_url", img);
                    item.put("link", headerLink);
                    contents.add(item);
                }
            }
            template.put("contents", contents);

            // 3. 버튼 오브젝트들 빌드 (1순위 탈출 추천, 2순위 변경 모달)
            List<Map<String, Object>> buttons = new ArrayList<>();
            
            Map<String, Object> btnEscape = new HashMap<>();
            btnEscape.put("title", "🔥 구식 싫어요! 외부 맛집 추천");
            
            Map<String, String> escapeLink = new HashMap<>();
            String encodedLoc = URLEncoder.encode(menu.getCafeteriaName(), StandardCharsets.UTF_8);
            String escapeUrl = "https://amugeona-buster-6eda848df67d.herokuapp.com/?escape=true&location=" + encodedLoc;
            escapeLink.put("web_url", escapeUrl);
            escapeLink.put("mobile_web_url", escapeUrl);
            
            btnEscape.put("link", escapeLink);
            buttons.add(btnEscape);

            Map<String, Object> btnSettings = new HashMap<>();
            btnSettings.put("title", "⚙️ 알림 시간/지점 변경");
            
            Map<String, String> settingsLink = new HashMap<>();
            String settingsUrl = "https://amugeona-buster-6eda848df67d.herokuapp.com/?welstory=true";
            settingsLink.put("web_url", settingsUrl);
            settingsLink.put("mobile_web_url", settingsUrl);
            
            btnSettings.put("link", settingsLink);
            buttons.add(btnSettings);

            template.put("buttons", buttons);

            // JSON 직렬화 및 카카오 전송
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String templateJson = mapper.writeValueAsString(template);
            
            log.info("Sending List Template json: {}", templateJson);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("template_object", templateJson);

            restClient.post()
                .uri("https://kapi.kakao.com/v2/api/talk/memo/default/send")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity();
            
            log.info("Successfully sent Welstory LIST menu KakaoTalk message!");
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send KakaoTalk LIST memo message: {}", e.getMessage(), e);
            return false;
        }
    }

    @Getter
    @Setter
    @ToString
    public static class KakaoTokenResponse {
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
}
