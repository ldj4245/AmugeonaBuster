package com.amugeonabuster.adapter.in.web;

import com.amugeonabuster.application.port.in.GetTodayMenuQuery;
import com.amugeonabuster.application.port.in.GetWelstoryAlertQuery;
import com.amugeonabuster.application.port.in.KakaoAuthCommand;
import com.amugeonabuster.application.port.in.SaveWelstoryAlertCommand;
import com.amugeonabuster.domain.model.WelstoryAlertSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AlertSecurityTest {
    private final GetWelstoryAlertQuery query = mock(GetWelstoryAlertQuery.class);
    private final KakaoAuthCommand auth = mock(KakaoAuthCommand.class);
    private final WelstoryAlertController controller = new WelstoryAlertController(query,
            mock(SaveWelstoryAlertCommand.class), mock(GetTodayMenuQuery.class), auth, "test-key", "https://example.com");

    @Test
    void anonymousSettingsReadIsRejected() {
        assertThatThrownBy(() -> controller.settings(new MockHttpSession())).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(query);
    }

    @Test
    void settingsResponseNeverIncludesOAuthTokens() throws Exception {
        var session = new MockHttpSession(); session.setAttribute("welstoryUser", "123");
        when(query.getSettings("123")).thenReturn(Optional.of(WelstoryAlertSettings.builder()
                .kakaoId("123").nickname("테스트").kakaoAccessToken("secret-access")
                .kakaoRefreshToken("secret-refresh").build()));
        String response = new ObjectMapper().writeValueAsString(controller.settings(session));
        assertThat(response).doesNotContain("secret-access", "secret-refresh", "kakaoAccessToken", "kakaoRefreshToken");
    }

    @Test
    void oauthRejectsForgedStateBeforeCallingProvider() {
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute("kakaoState", "expected");
        assertThatThrownBy(() -> controller.exchange(new WelstoryAlertController.TokenExchangeRequest(
                "code", "forged", "https://example.com"), request)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(auth);
    }
}
