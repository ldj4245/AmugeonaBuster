package com.amugeonabuster.adapter.in.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 프론트엔드가 양방향 커넥션을 맺을 웹소켓 엔드포인트 등록
        registry.addEndpoint("/ws-connection")
                .setAllowedOriginPatterns("*") // CORS 허용 (로컬 React 가동용)
                .withSockJS(); // SockJS 지원
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 -> 클라이언트로 메시지를 쏴주는 구독(Subscribe) 브로커 등록
        registry.enableSimpleBroker("/topic");
        
        // 클라이언트 -> 서버로 메시지 보낼 때 라우팅될 접두사
        registry.setApplicationDestinationPrefixes("/app");
    }
}
