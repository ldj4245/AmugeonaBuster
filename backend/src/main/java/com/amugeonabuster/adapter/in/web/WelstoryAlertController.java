package com.amugeonabuster.adapter.in.web;

import com.amugeonabuster.application.port.in.GetTodayMenuQuery;
import com.amugeonabuster.application.port.in.GetWelstoryAlertQuery;
import com.amugeonabuster.application.port.in.KakaoAuthCommand;
import com.amugeonabuster.application.port.in.SaveWelstoryAlertCommand;
import com.amugeonabuster.domain.model.WelstoryAlertSettings;
import com.amugeonabuster.domain.model.WelstoryMenuResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/welstory")
public class WelstoryAlertController {
    private final GetWelstoryAlertQuery query;
    private final SaveWelstoryAlertCommand command;
    private final GetTodayMenuQuery menuQuery;
    private final KakaoAuthCommand auth;
    private final String restKey;
    private final String publicUrl;
    private static final String USER = "welstoryUser";
    private static final String STATE = "kakaoState";
    private static final String REDIRECT = "kakaoRedirect";

    public WelstoryAlertController(GetWelstoryAlertQuery query, SaveWelstoryAlertCommand command,
            GetTodayMenuQuery menuQuery, KakaoAuthCommand auth,
            @Value("${kakao.api.rest-key}") String restKey,
            @Value("${app.public-url:https://amugeona-buster-6eda848df67d.herokuapp.com}") String publicUrl) {
        this.query = query;
        this.command = command;
        this.menuQuery = menuQuery;
        this.auth = auth;
        this.restKey = restKey;
        this.publicUrl = publicUrl;
    }

    public record SettingsResponse(String nickname, String cotNo, String hallNo, String cafeteriaName,
            String scheduledDays, String scheduledTime, boolean enabled) {
        static SettingsResponse from(WelstoryAlertSettings settings) {
            return new SettingsResponse(settings.getNickname(), settings.getCotNo(), settings.getHallNo(),
                    settings.getCafeteriaName(), settings.getScheduledDays(), settings.getScheduledTime(), settings.isEnabled());
        }
    }
    public record SaveSettingsRequest(String cotNo, String hallNo, String cafeteriaName,
            String scheduledDays, String scheduledTime, boolean isEnabled) {}
    public record TokenExchangeRequest(String code, String state, String redirectUri) {}
    public record CafeteriaPreset(String name, String cotNo, String hallNo) {}

    private WelstoryAlertSettings current(HttpSession session) {
        Object id = session.getAttribute(USER);
        if (!(id instanceof String)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 연결이 필요합니다.");
        return query.getSettings((String) id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @GetMapping("/auth-url")
    public Map<String, String> authUrl(@RequestParam("redirectUri") String redirectUri, HttpSession session) {
        if (restKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "카카오 연결을 준비 중이에요. 잠시 후 다시 시도해 주세요.");
        if (!List.of(publicUrl, "http://localhost:5173", "http://localhost:8080", "http://127.0.0.1:5173").contains(redirectUri)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않는 연결 주소입니다.");
        }
        String state = UUID.randomUUID().toString();
        session.setAttribute(STATE, state);
        session.setAttribute(REDIRECT, redirectUri);
        return Map.of("url", "https://kauth.kakao.com/oauth/authorize?client_id=" + restKey
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=code&scope=talk_message&state=" + state);
    }

    @PostMapping("/token-exchange")
    public SettingsResponse exchange(@RequestBody TokenExchangeRequest request, HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession();
        if (request.state() == null || !request.state().equals(session.getAttribute(STATE))
                || !request.redirectUri().equals(session.getAttribute(REDIRECT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "연결 요청이 만료됐어요. 카카오 연결을 다시 시작해 주세요.");
        }
        session.removeAttribute(STATE);
        session.removeAttribute(REDIRECT);
        var tokens = auth.getOAuthTokens(request.code(), request.redirectUri());
        var profile = auth.getUserMe(tokens.getAccessToken());
        String id = String.valueOf(profile.getId());
        WelstoryAlertSettings settings = query.getSettings(id).orElseGet(() -> WelstoryAlertSettings.builder()
                .kakaoId(id).cotNo("WEL_DSR").hallNo("HALL_01").cafeteriaName("삼성 DSR 타워 웰스토리")
                .scheduledDays("1,2,3,4,5").scheduledTime("11:30").isEnabled(true).build());
        settings.setNickname(profile.getNickname());
        settings.setKakaoAccessToken(tokens.getAccessToken());
        if (tokens.getRefreshToken() != null) settings.setKakaoRefreshToken(tokens.getRefreshToken());
        SettingsResponse response = SettingsResponse.from(command.saveSettings(settings));
        servletRequest.changeSessionId();
        session.setAttribute(USER, id);
        session.setMaxInactiveInterval(60 * 60 * 24 * 7);
        return response;
    }

    @GetMapping("/settings")
    public SettingsResponse settings(HttpSession session) { return SettingsResponse.from(current(session)); }

    @PostMapping("/settings")
    public SettingsResponse save(@RequestBody SaveSettingsRequest request, HttpSession session) {
        WelstoryAlertSettings settings = current(session);
        var preset = cafeterias().stream().filter(c -> c.cotNo().equals(request.cotNo()) && c.hallNo().equals(request.hallNo()))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "식당을 선택해 주세요."));
        if (request.scheduledDays() == null || !request.scheduledDays().matches("[1-7](,[1-7]){0,6}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "받을 요일을 선택해 주세요.");
        }
        try {
            if (request.scheduledTime() == null || !request.scheduledTime().matches("\\d{2}:\\d{2}")) throw new IllegalArgumentException();
            LocalTime.parse(request.scheduledTime());
        } catch (RuntimeException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "알림 시간을 확인해 주세요."); }
        settings.setCotNo(preset.cotNo()); settings.setHallNo(preset.hallNo()); settings.setCafeteriaName(preset.name());
        settings.setScheduledDays(request.scheduledDays()); settings.setScheduledTime(request.scheduledTime()); settings.setEnabled(request.isEnabled());
        return SettingsResponse.from(command.saveSettings(settings));
    }

    @PostMapping("/test-send")
    public Map<String, Object> test(HttpSession session) throws Exception {
        WelstoryAlertSettings settings = current(session);
        Long last = (Long) session.getAttribute("lastTestSend");
        if (last != null && System.currentTimeMillis() - last < 60000) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "1분 후 다시 시도해 주세요.");
        session.setAttribute("lastTestSend", System.currentTimeMillis());
        if (!command.triggerTestSend(settings.getKakaoId())) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "식단을 보내지 못했어요. 잠시 후 다시 시도해 주세요.");
        return Map.of("success", true);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        WelstoryAlertSettings settings = current(session);
        settings.setEnabled(false);
        command.saveSettings(settings);
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cafeterias")
    public List<CafeteriaPreset> cafeterias() {
        return List.of(new CafeteriaPreset("삼성 DSR 타워 웰스토리", "WEL_DSR", "HALL_01"),
                new CafeteriaPreset("삼성전자 수원디지털시티 R5", "WEL_SUWON", "HALL_02"),
                new CafeteriaPreset("삼성전자 기흥캠퍼스 MR1", "WEL_GIHEUNG", "HALL_03"),
                new CafeteriaPreset("삼성전자 화성캠퍼스 D1", "WEL_HWASEONG", "HALL_04"),
                new CafeteriaPreset("삼성전자 서초사옥 웰스토리", "WEL_SEOCHO", "HALL_05"),
                new CafeteriaPreset("삼성웰스토리 본사 식당", "WEL_HQ", "HALL_06"));
    }

    @GetMapping("/menu-details")
    public WelstoryMenuResult menu(@RequestParam("cotNo") String cotNo, @RequestParam("hallNo") String hallNo,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh) {
        var preset = cafeterias().stream().filter(c -> c.cotNo().equals(cotNo) && c.hallNo().equals(hallNo))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 식당입니다."));
        if (date == null) {
            return refresh ? menuQuery.refreshMenu(cotNo, hallNo, preset.name(), java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")))
                    : menuQuery.getTodayMenu(cotNo, hallNo, preset.name());
        }
        try {
            var parsedDate = java.time.LocalDate.parse(date);
            return refresh ? menuQuery.refreshMenu(cotNo, hallNo, preset.name(), parsedDate)
                    : menuQuery.getMenu(cotNo, hallNo, preset.name(), parsedDate);
        } catch (java.time.format.DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "날짜 형식이 올바르지 않습니다.");
        }
    }
}
