package com.amugeonabuster.application.service;

import com.amugeonabuster.application.port.in.KakaoAuthCommand;
import com.amugeonabuster.application.port.out.KakaoApiPort;
import com.amugeonabuster.application.port.out.KakaoApiPort.KakaoOAuthResponse;
import com.amugeonabuster.application.port.out.KakaoApiPort.KakaoUserMeResponse;
import com.amugeonabuster.application.port.out.KakaoApiPort.TokenRefreshResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KakaoMessageService implements KakaoAuthCommand {

    private final KakaoApiPort kakaoApiPort;

    @Override
    public KakaoOAuthResponse getOAuthTokens(String code, String redirectUri) {
        return kakaoApiPort.fetchOAuthTokens(code, redirectUri);
    }

    @Override
    public KakaoUserMeResponse getUserMe(String accessToken) {
        return kakaoApiPort.fetchUserProfile(accessToken);
    }

    @Override
    public TokenRefreshResult refreshTokens(String refreshToken) throws Exception {
        return kakaoApiPort.refreshOAuthTokens(refreshToken);
    }
}
