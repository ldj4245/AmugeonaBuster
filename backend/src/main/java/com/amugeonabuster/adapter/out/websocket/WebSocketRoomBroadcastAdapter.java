package com.amugeonabuster.adapter.out.websocket;

import com.amugeonabuster.adapter.out.websocket.dto.WebSocketRoomResponse;
import com.amugeonabuster.application.port.out.BroadcastRoomStatePort;
import com.amugeonabuster.domain.model.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketRoomBroadcastAdapter implements BroadcastRoomStatePort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastRoomState(Room room) {
        log.info("[Broadcast] room={}, status={}, swipesCount={}", room.getId(), room.getStatus(), room.getSwipes().size());

        WebSocketRoomResponse response = WebSocketRoomResponse.fromDomain(room);

        log.info("[Broadcast] voteStats count={}", response.getVoteStats() != null ? response.getVoteStats().size() : "null");
        if (response.getVoteStats() != null) {
            response.getVoteStats().forEach(stat ->
                log.info("[Broadcast] menu={}, likes={}, dislikes={}", stat.getMenuName(), stat.getLikes(), stat.getDislikes())
            );
        }

        String destination = "/topic/rooms/" + room.getId();

        // 해당 방 코드를 구독하고 있는 모든 유저 브라우저 세션에 실시간 브로드캐스트 전송!
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { messagingTemplate.convertAndSend(destination, response); }
            });
        } else {
            messagingTemplate.convertAndSend(destination, response);
        }
    }
}
