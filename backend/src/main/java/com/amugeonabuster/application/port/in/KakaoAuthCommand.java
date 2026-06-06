package com.amugeonabuster.application.port.in;

import com.amugeonabuster.application.port.out.KakaoApiPort.KakaoOAuthResponse;
import com.amugeonabuster.application.port.out.KakaoApiPort.KakaoUserMeResponse;
import com.amugeonabuster.application.port.out.KakaoApiPort.TokenRefreshResult;

public interface KakaoAuthCommand {
    KakaoOAuthResponse getOAuthTokens(String code, String redirectUri);
    KakaoUserMeResponse getUserMe(String accessToken);
    TokenRefreshResult refreshTokens(String refreshToken) throws Exception;
}
