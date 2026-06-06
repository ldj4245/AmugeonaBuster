package com.amugeonabuster.adapter.in.web;

import com.amugeonabuster.application.port.in.GetTodayMenuQuery;
import com.amugeonabuster.application.port.in.GetWelstoryAlertQuery;
import com.amugeonabuster.application.port.in.KakaoAuthCommand;
import com.amugeonabuster.application.port.in.SaveWelstoryAlertCommand;
import com.amugeonabuster.application.port.out.KakaoApiPort.KakaoOAuthResponse;
import com.amugeonabuster.application.port.out.KakaoApiPort.KakaoUserMeResponse;
import com.amugeonabuster.domain.model.WelstoryAlertSettings;
import com.amugeonabuster.domain.model.WelstoryMenuResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/welstory")
@Slf4j
public class WelstoryAlertController {

    private final GetWelstoryAlertQuery getWelstoryAlertQuery;
    private final SaveWelstoryAlertCommand saveWelstoryAlertCommand;
    private final GetTodayMenuQuery menuQuery;
    private final KakaoAuthCommand kakaoAuthCommand;
    private final String restKey;

    public WelstoryAlertController(
            GetWelstoryAlertQuery getWelstoryAlertQuery,
            SaveWelstoryAlertCommand saveWelstoryAlertCommand,
            GetTodayMenuQuery menuQuery,
            KakaoAuthCommand kakaoAuthCommand,
            @Value("${kakao.api.rest-key}") String restKey
    ) {
        this.getWelstoryAlertQuery = getWelstoryAlertQuery;
        this.saveWelstoryAlertCommand = saveWelstoryAlertCommand;
        this.menuQuery = menuQuery;
        this.kakaoAuthCommand = kakaoAuthCommand;
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
        private String scheduledDays;
        private String scheduledTime;
        
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

    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl(@RequestParam("redirectUri") String redirectUri) {
        log.info("Generating Kakao OAuth Auth URL. RedirectUri: {}", redirectUri);
        String url = "https://kauth.kakao.com/oauth/authorize"
            + "?client_id=" + restKey
            + "&redirect_uri=" + redirectUri
            + "&response_type=code"
            + "&scope=talk_message";
        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/token-exchange")
    public ResponseEntity<WelstoryAlertSettings> tokenExchange(@RequestBody TokenExchangeRequest request) {
        log.info("Performing Kakao token exchange for code: {}", request.getCode());
        try {
            KakaoOAuthResponse tokens = kakaoAuthCommand.getOAuthTokens(request.getCode(), request.getRedirectUri());
            KakaoUserMeResponse profile = kakaoAuthCommand.getUserMe(tokens.getAccessToken());

            String kakaoId = String.valueOf(profile.getId());
            String nickname = profile.getNickname();

            WelstoryAlertSettings entity = getWelstoryAlertQuery.getSettings(kakaoId)
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

            WelstoryAlertSettings saved = saveWelstoryAlertCommand.saveSettings(entity);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Failed to perform token exchange: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/settings")
    public ResponseEntity<WelstoryAlertSettings> getSettings(@RequestParam("kakaoId") String kakaoId) {
        log.info("Viewing Welstory settings for kakaoId: {}", kakaoId);
        return getWelstoryAlertQuery.getSettings(kakaoId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/settings")
    public ResponseEntity<WelstoryAlertSettings> saveSettings(@RequestBody SaveSettingsRequest request) {
        log.info("Saving Welstory settings for user: {}, kakaoId: {}", request.getNickname(), request.getKakaoId());

        WelstoryAlertSettings entity = getWelstoryAlertQuery.getSettings(request.getKakaoId())
            .orElseGet(() -> WelstoryAlertSettings.builder().kakaoId(request.getKakaoId()).build());

        entity.setNickname(request.getNickname());
        entity.setKakaoAccessToken(request.getKakaoAccessToken());
        entity.setKakaoRefreshToken(request.getKakaoRefreshToken());
        entity.setCotNo(request.getCotNo());
        entity.setHallNo(request.getHallNo());
        entity.setCafeteriaName(request.getCafeteriaName());
        entity.setScheduledDays(request.getScheduledDays());
        entity.setScheduledTime(request.getScheduledTime());
        entity.setEnabled(request.isEnabled());

        WelstoryAlertSettings saved = saveWelstoryAlertCommand.saveSettings(entity);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/test-send")
    public ResponseEntity<Map<String, Object>> testSend(@RequestParam("kakaoId") String kakaoId) {
        log.info("Triggering immediate test Welstory send for kakaoId: {}", kakaoId);
        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = saveWelstoryAlertCommand.triggerTestSend(kakaoId);
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

    @PostMapping("/reset-last-sent")
    public ResponseEntity<Map<String, Object>> resetLastSent(@RequestParam(value = "kakaoId", required = false) String kakaoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (kakaoId == null || kakaoId.trim().isEmpty()) {
                log.info("Resetting lastSentDate for all users.");
                saveWelstoryAlertCommand.resetAllLastSentDates();
                response.put("message", "전체 사용자의 오늘 자 발송 완료 플래그가 강제 초기화되었습니다! 🔄");
            } else {
                log.info("Resetting lastSentDate for kakaoId: {}", kakaoId);
                saveWelstoryAlertCommand.resetLastSentDate(kakaoId);
                response.put("message", "해당 사용자의 오늘 자 발송 완료 플래그가 강제 초기화되었습니다! 🔄");
            }
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to reset lastSentDate: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "초기화 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/menu-details")
    public ResponseEntity<WelstoryMenuResult> getMenuDetails(
            @RequestParam("cotNo") String cotNo,
            @RequestParam("hallNo") String hallNo,
            @RequestParam("cafeteriaName") String cafeteriaName
    ) {
        log.info("Viewing real-time Welstory menu details for cotNo: {}, hallNo: {}", cotNo, hallNo);
        try {
            WelstoryMenuResult menuResult = menuQuery.getTodayMenu(cotNo, hallNo, cafeteriaName);
            return ResponseEntity.ok(menuResult);
        } catch (Exception e) {
            log.error("Failed to fetch Welstory menu details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
