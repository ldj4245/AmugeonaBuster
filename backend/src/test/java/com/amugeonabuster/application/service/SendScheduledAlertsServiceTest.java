package com.amugeonabuster.application.service;

import com.amugeonabuster.application.port.in.GetTodayMenuQuery;
import com.amugeonabuster.application.port.out.KakaoApiPort;
import com.amugeonabuster.application.port.out.KakaoApiPort.SendStatus;
import com.amugeonabuster.application.port.out.KakaoApiPort.TokenRefreshResult;
import com.amugeonabuster.application.port.out.WelstoryAlertPort;
import com.amugeonabuster.domain.model.WelstoryAlertSettings;
import com.amugeonabuster.domain.model.WelstoryMenuResult;
import com.amugeonabuster.domain.model.WelstoryMenuResult.CourseMenu;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SendScheduledAlertsServiceTest {

    private final WelstoryAlertPort alertPort = mock(WelstoryAlertPort.class);
    private final GetTodayMenuQuery menuQuery = mock(GetTodayMenuQuery.class);
    private final KakaoApiPort kakaoApiPort = mock(KakaoApiPort.class);
    private final SendScheduledAlertsService service = new SendScheduledAlertsService(alertPort, menuQuery, kakaoApiPort);

    private WelstoryAlertSettings activeSettingForNow() {
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        String nowStr = LocalTime.now(seoul).format(DateTimeFormatter.ofPattern("HH:mm"));
        String todayDow = String.valueOf(LocalDate.now(seoul).getDayOfWeek().getValue());
        return WelstoryAlertSettings.builder()
                .id(1L)
                .kakaoId("user-1")
                .nickname("테스터")
                .kakaoAccessToken("old-access")
                .kakaoRefreshToken("old-refresh")
                .cotNo("COT")
                .hallNo("HALL")
                .cafeteriaName("구내식당")
                .scheduledTime(nowStr)
                .scheduledDays(todayDow)
                .isEnabled(true)
                .build();
    }

    private WelstoryMenuResult partialMenuWithCourse() {
        return WelstoryMenuResult.builder()
                .cafeteriaName("구내식당")
                .status("PARTIAL")
                .courses(List.of(CourseMenu.builder().courseName("A코스 (점심)").menuDetails("불고기").build()))
                .build();
    }

    @Test
    void sendsAlertEvenWhenOverallMenuStatusIsPartialIfTargetSlotCoursesExist() {
        WelstoryAlertSettings setting = activeSettingForNow();
        WelstoryMenuResult partialMenu = partialMenuWithCourse();

        when(alertPort.findAllByScheduledTimeAndIsEnabled(anyString())).thenReturn(List.of(setting));
        when(menuQuery.getTodayMenu(anyString(), anyString(), anyString())).thenReturn(partialMenu);
        when(menuQuery.filterMenuByTime(any(), anyString())).thenReturn(partialMenu);
        when(kakaoApiPort.sendWelstoryMenu(eq("old-access"), any(), anyString(), anyString())).thenReturn(SendStatus.SENT);

        service.runScheduledWelstoryAlerts();

        assertThat(setting.isEnabled()).isTrue();
        assertThat(setting.getLastSentDate()).isEqualTo(LocalDate.now(ZoneId.of("Asia/Seoul")));
        verify(alertPort).save(setting);
    }

    @Test
    void doesNotDisableSubscriptionOnTransientSendFailure() {
        WelstoryAlertSettings setting = activeSettingForNow();
        WelstoryMenuResult menu = partialMenuWithCourse();

        when(alertPort.findAllByScheduledTimeAndIsEnabled(anyString())).thenReturn(List.of(setting));
        when(menuQuery.getTodayMenu(anyString(), anyString(), anyString())).thenReturn(menu);
        when(menuQuery.filterMenuByTime(any(), anyString())).thenReturn(menu);
        when(kakaoApiPort.sendWelstoryMenu(anyString(), any(), anyString(), anyString())).thenReturn(SendStatus.FAILED);

        service.runScheduledWelstoryAlerts();

        assertThat(setting.isEnabled()).isTrue();
        assertThat(setting.getLastSentDate()).isNull();
        verify(kakaoApiPort, never()).refreshOAuthTokens(anyString());
    }

    @Test
    void refreshesTokenAndRetriesWhenSendReturnsUnauthorized() {
        WelstoryAlertSettings setting = activeSettingForNow();
        WelstoryMenuResult menu = partialMenuWithCourse();

        when(alertPort.findAllByScheduledTimeAndIsEnabled(anyString())).thenReturn(List.of(setting));
        when(menuQuery.getTodayMenu(anyString(), anyString(), anyString())).thenReturn(menu);
        when(menuQuery.filterMenuByTime(any(), anyString())).thenReturn(menu);
        when(kakaoApiPort.sendWelstoryMenu(eq("old-access"), any(), anyString(), anyString()))
                .thenReturn(SendStatus.UNAUTHORIZED);
        when(kakaoApiPort.refreshOAuthTokens("old-refresh"))
                .thenReturn(TokenRefreshResult.builder().accessToken("new-access").refreshToken("new-refresh").build());
        when(kakaoApiPort.sendWelstoryMenu(eq("new-access"), any(), anyString(), anyString()))
                .thenReturn(SendStatus.SENT);

        service.runScheduledWelstoryAlerts();

        assertThat(setting.isEnabled()).isTrue();
        assertThat(setting.getKakaoAccessToken()).isEqualTo("new-access");
        assertThat(setting.getLastSentDate()).isEqualTo(LocalDate.now(ZoneId.of("Asia/Seoul")));
    }

    @Test
    void disablesSubscriptionWhenRefreshTokenIsRejectedByKakao() {
        WelstoryAlertSettings setting = activeSettingForNow();
        WelstoryMenuResult menu = partialMenuWithCourse();

        when(alertPort.findAllByScheduledTimeAndIsEnabled(anyString())).thenReturn(List.of(setting));
        when(menuQuery.getTodayMenu(anyString(), anyString(), anyString())).thenReturn(menu);
        when(menuQuery.filterMenuByTime(any(), anyString())).thenReturn(menu);
        when(kakaoApiPort.sendWelstoryMenu(eq("old-access"), any(), anyString(), anyString()))
                .thenReturn(SendStatus.UNAUTHORIZED);
        when(kakaoApiPort.refreshOAuthTokens("old-refresh"))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST, "invalid_grant", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));

        service.runScheduledWelstoryAlerts();

        assertThat(setting.isEnabled()).isFalse();
        verify(alertPort).save(setting);
    }
}
