package com.amugeonabuster.adapter.out.websocket;

import com.amugeonabuster.adapter.out.websocket.dto.WebSocketRoomResponse;
import com.amugeonabuster.application.port.out.BroadcastRoomStatePort;
import com.amugeonabuster.domain.model.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketRoomBroadcastAdapter implements BroadcastRoomStatePort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastRoomState(Room room) {
        WebSocketRoomResponse response = WebSocketRoomResponse.fromDomain(room);
        String destination = "/topic/rooms/" + room.getId();
        
        // 해당 방 코드를 구독하고 있는 모든 유저 브라우저 세션에 실시간 브로드캐스트 전송!
        messagingTemplate.convertAndSend(destination, response);
    }
}
