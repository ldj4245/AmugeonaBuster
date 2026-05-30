package com.amugeonabuster.adapter.out.websocket.dto;

import com.amugeonabuster.domain.model.DefaultMenus;
import com.amugeonabuster.domain.model.Room;
import com.amugeonabuster.domain.model.RoomStatus;
import com.amugeonabuster.domain.model.Swipe;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
public class WebSocketRoomResponse {
    private final String roomId;
    private final UUID hostId;
    private final String location;
    private final RoomStatus status;
    private final List<WebSocketMemberResponse> members;
    private final String winningMenu;
    private final List<WebSocketRestaurantResponse> matchedRestaurants;
    private final List<String> defaultMenus;
    private final int totalMembers;
    private final int completedMembersCount;

    /**
     * 도메인 객체로부터 웹소켓 응답 포맷으로 고속 변환
     */
    public static WebSocketRoomResponse fromDomain(Room room) {
        if (room == null) {
            return null;
        }

        List<WebSocketMemberResponse> memberResponses = room.getMembers().stream()
                .map(m -> WebSocketMemberResponse.builder()
                        .id(m.getId())
                        .nickname(m.getNickname())
                        .isReady(m.isReady())
                        .build())
                .collect(Collectors.toList());

        List<WebSocketRestaurantResponse> restaurantResponses = room.getMatchedRestaurants().stream()
                .map(WebSocketRestaurantResponse::fromDomain)
                .collect(Collectors.toList());

        // 각 멤버별 스와이프 완료 여부를 집계하여 완료자 인원수 계산
        int targetMenuCount = DefaultMenus.MENUS.size();
        long completedCount = room.getMembers().stream()
                .filter(m -> {
                    long uniqueSwiped = room.getSwipes().stream()
                            .filter(s -> s.getMemberId().equals(m.getId()))
                            .map(Swipe::getMenuName)
                            .distinct()
                            .count();
                    return uniqueSwiped >= targetMenuCount;
                })
                .count();

        return WebSocketRoomResponse.builder()
                .roomId(room.getId())
                .hostId(room.getHostId())
                .location(room.getLocation())
                .status(room.getStatus())
                .members(memberResponses)
                .winningMenu(room.getWinningMenu())
                .matchedRestaurants(restaurantResponses)
                .defaultMenus(DefaultMenus.MENUS)
                .totalMembers(room.getMembers().size())
                .completedMembersCount((int) completedCount)
                .build();
    }
}
