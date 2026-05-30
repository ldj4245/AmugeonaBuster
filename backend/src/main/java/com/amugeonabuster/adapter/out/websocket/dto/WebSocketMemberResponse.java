package com.amugeonabuster.adapter.out.websocket.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class WebSocketMemberResponse {
    private final UUID id;
    private final String nickname;
    private final boolean isReady;
}
