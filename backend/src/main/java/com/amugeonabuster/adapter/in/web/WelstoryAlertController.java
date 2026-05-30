package com.amugeonabuster.adapter.in.web;

import com.amugeonabuster.domain.model.WelstoryAlertSettings;
import com.amugeonabuster.application.port.in.WelstoryAlertUseCase;
import com.amugeonabuster.application.service.KakaoMessageService;
import com.amugeonabuster.application.service.WelstoryMenuService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.annotation.JsonProperty;

@RestController
@RequestMapping("/api/welstory")
@Slf4j
public class WelstoryAlertController {

    private final WelstoryAlertUseCase welstoryAlertUseCase;
    private final WelstoryMenuService menuService;
    private final KakaoMessageService kakaoMessageService;
    private final String restKey;

    public WelstoryAlertController(
            WelstoryAlertUseCase welstoryAlertUseCase,
            WelstoryMenuService menuService,
            KakaoMessageService kakaoMessageService,
            @Value("${kakao.api.rest-key}") String restKey
    ) {
        this.welstoryAlertUseCase = welstoryAlertUseCase;
        this.menuService = menuService;
        this.kakaoMessageService = kakaoMessageService;
        this.restKey = restKey;
    }

    @Data
    public static class SaveSettingsRequest {
        private String kakaoId;
        private String nickname;
        private String kakaoAccessToken;
        private String kakaoRefreshToken;
        private String cotNo;
        private String hallNo;
        private String cafeteriaName;
        private String scheduledDays; // e.g., "1,2,3,4,5"
        private String scheduledTime; // e.g., "11:30"
        
        @JsonProperty("isEnabled")
        private boolean isEnabled;
    }

    @Data
    public static class TokenExchangeRequest {
        private String code;
        private String redirectUri;
    }

    @Data
    @Builder
    public static class CafeteriaPreset {
        private String name;
        private String cotNo;
        private String hallNo;
    }

    /**
     * 카카오 OAuth 인증 URL을 발급합니다.
     */
    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl(@RequestParam("redirectUri") String redirectUri) {
        log.info("Generating Kakao OAuth Auth URL. RedirectUri: {}", redirectUri);
        String url = "https://kauth.kakao.com/oauth/authorize"
            + "?client_id=" + restKey
            + "&redirect_uri=" + redirectUri
            + "&response_type=code"
            + "&scope=talk_message"; // talk_message는 필수
        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        return ResponseEntity.ok(response);
    }

    /**
     * 카카오 OAuth 인증 코드를 토큰으로 교환하고 기본 알림 설정을 저장/로드합니다.
     */
    @PostMapping("/token-exchange")
    public ResponseEntity<WelstoryAlertSettings> tokenExchange(@RequestBody TokenExchangeRequest request) {
        log.info("Performing Kakao token exchange for code: {}", request.getCode());
        try {
            KakaoMessageService.KakaoOAuthResponse tokens = kakaoMessageService.getOAuthTokens(request.getCode(), request.getRedirectUri());
            KakaoMessageService.KakaoUserMeResponse profile = kakaoMessageService.getUserMe(tokens.getAccessToken());

            String kakaoId = String.valueOf(profile.getId());
            String nickname = profile.getProperties() != null ? profile.getProperties().getNickname() : "사용자";

            WelstoryAlertSettings entity = welstoryAlertUseCase.getSettings(kakaoId)
                .orElseGet(() -> WelstoryAlertSettings.builder()
                    .kakaoId(kakaoId)
                    .cotNo("WEL_DSR")
                    .hallNo("HALL_01")
                    .cafeteriaName("삼성 DSR 타워 웰스토리")
                    .scheduledDays("1,2,3,4,5")
                    .scheduledTime("11:30")
                    .isEnabled(true)
                    .build());

            entity.setNickname(nickname);
            entity.setKakaoAccessToken(tokens.getAccessToken());
            entity.setKakaoRefreshToken(tokens.getRefreshToken());

            WelstoryAlertSettings saved = welstoryAlertUseCase.saveSettings(entity);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Failed to perform token exchange: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 특정 카카오 유저의 웰스토리 알림 설정을 가져옵니다.
     */
    @GetMapping("/settings")
    public ResponseEntity<WelstoryAlertSettings> getSettings(@RequestParam("kakaoId") String kakaoId) {
        log.info("Fetching Welstory settings for kakaoId: {}", kakaoId);
        return welstoryAlertUseCase.getSettings(kakaoId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * 웰스토리 알림 설정을 저장하거나 업데이트합니다.
     */
    @PostMapping("/settings")
    public ResponseEntity<WelstoryAlertSettings> saveSettings(@RequestBody SaveSettingsRequest request) {
        log.info("Saving Welstory settings for user: {}, kakaoId: {}", request.getNickname(), request.getKakaoId());

        WelstoryAlertSettings entity = welstoryAlertUseCase.getSettings(request.getKakaoId())
            .orElseGet(() -> WelstoryAlertSettings.builder().kakaoId(request.getKakaoId()).build());

        // 값 업데이트
        entity.setNickname(request.getNickname());
        entity.setKakaoAccessToken(request.getKakaoAccessToken());
        entity.setKakaoRefreshToken(request.getKakaoRefreshToken());
        entity.setCotNo(request.getCotNo());
        entity.setHallNo(request.getHallNo());
        entity.setCafeteriaName(request.getCafeteriaName());
        entity.setScheduledDays(request.getScheduledDays());
        entity.setScheduledTime(request.getScheduledTime());
        entity.setEnabled(request.isEnabled());

        WelstoryAlertSettings saved = welstoryAlertUseCase.saveSettings(entity);
        return ResponseEntity.ok(saved);
    }

    /**
     * 설정 테스트를 위해 오늘 구내식당 식단을 사용자 카카오톡으로 지금 즉시 발송합니다.
     */
    @PostMapping("/test-send")
    public ResponseEntity<Map<String, Object>> testSend(@RequestParam("kakaoId") String kakaoId) {
        log.info("Triggering immediate test Welstory send for kakaoId: {}", kakaoId);
        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = welstoryAlertUseCase.triggerTestSend(kakaoId);
            if (success) {
                response.put("success", true);
                response.put("message", "성공적으로 테스트 메시지가 카카오톡으로 발송되었습니다! 카톡을 확인해 보세요. 🍱");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "메시지 발송에 실패했습니다. 카카오톡 전송 허용 권한(talk_message)을 동의했는지 확인해 주세요.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Exception during test send for kakaoId {}: {}", kakaoId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "카카오 로그인 세션이 만료되었거나 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 편리한 구내식당 코드 입력을 위한 사전 정의된 주요 삼성 웰스토리 지점 코드 목록을 조회합니다.
     */
    @GetMapping("/cafeterias")
    public ResponseEntity<List<CafeteriaPreset>> getCafeterias() {
        List<CafeteriaPreset> presets = new ArrayList<>();
        presets.add(CafeteriaPreset.builder().name("삼성 DSR 타워 웰스토리").cotNo("WEL_DSR").hallNo("HALL_01").build());
        presets.add(CafeteriaPreset.builder().name("삼성전자 수원디지털시티 R5").cotNo("WEL_SUWON").hallNo("HALL_02").build());
        presets.add(CafeteriaPreset.builder().name("삼성전자 기흥캠퍼스 MR1").cotNo("WEL_GIHEUNG").hallNo("HALL_03").build());
        presets.add(CafeteriaPreset.builder().name("삼성전자 화성캠퍼스 D1").cotNo("WEL_HWASEONG").hallNo("HALL_04").build());
        presets.add(CafeteriaPreset.builder().name("삼성전자 서초사옥 웰스토리").cotNo("WEL_SEOCHO").hallNo("HALL_05").build());
        presets.add(CafeteriaPreset.builder().name("삼성웰스토리 본사 식당").cotNo("WEL_HQ").hallNo("HALL_06").build());
        return ResponseEntity.ok(presets);
    }
}
