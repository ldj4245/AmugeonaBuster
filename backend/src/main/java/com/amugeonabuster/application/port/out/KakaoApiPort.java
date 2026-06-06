package com.amugeonabuster.application.port.out;

import com.amugeonabuster.domain.model.WelstoryMenuResult;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

public interface KakaoApiPort {

    @Getter
    @Builder
    @ToString
    class TokenRefreshResult {
        private final String accessToken;
        private final String refreshToken;
        private final Integer expiresIn;
    }

    @Getter
    @Builder
    @ToString
    class KakaoOAuthResponse {
        private final String accessToken;
        private final String refreshToken;
    }

    @Getter
    @Builder
    @ToString
    class KakaoUserMeResponse {
        private final Long id;
        private final String nickname;
    }

    KakaoOAuthResponse fetchOAuthTokens(String code, String redirectUri);

    KakaoUserMeResponse fetchUserProfile(String accessToken);

    TokenRefreshResult refreshOAuthTokens(String refreshToken);

    boolean sendWelstoryMenuToMe(String accessToken, WelstoryMenuResult menu, String cotNo, String hallNo);
}
