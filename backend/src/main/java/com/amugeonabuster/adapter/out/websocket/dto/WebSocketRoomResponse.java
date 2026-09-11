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
    private final java.time.Instant createdAt;
    private final UUID selectedRestaurantId;
    private final String location;
    private final String locationAddress;
    private final String locationPlaceId;
    private final Double latitude;
    private final Double longitude;
    private final RoomStatus status;
    private final List<WebSocketMemberResponse> members;
    private final String winningMenu;
    private final List<WebSocketRestaurantResponse> matchedRestaurants;
    private final List<String> defaultMenus;
    private final int totalMembers;
    private final int completedMembersCount;
    private final List<MenuVoteStat> voteStats;
    private final int maxSwipeCount;

    @Getter
    @Builder
    public static class MenuVoteStat {
        private final String menuName;
        private final long likes;
        private final long dislikes;
    }

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
        int targetMenuCount = room.getMaxSwipeCount();
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

        // 메뉴별 투표 통계 집계 (좋아요/싫어요) — broadcastRoomState 시점에 room이 in-memory swipes를 보유하고 있으므로 정확히 집계됨
        List<MenuVoteStat> voteStats = room.getCustomMenus().stream()
                .map(menu -> {
                    long likes = room.getSwipes().stream()
                            .filter(s -> s.getMenuName().equals(menu) && s.isLike())
                            .count();
                    long dislikes = room.getSwipes().stream()
                            .filter(s -> s.getMenuName().equals(menu) && !s.isLike())
                            .count();
                    return MenuVoteStat.builder()
                            .menuName(menu)
                            .likes(likes)
                            .dislikes(dislikes)
                            .build();
                })
                .filter(stat -> stat.getLikes() > 0 || stat.getDislikes() > 0)
                .sorted((a, b) -> Long.compare(b.getLikes(), a.getLikes()))
                .collect(Collectors.toList());

        return WebSocketRoomResponse.builder()
                .roomId(room.getId())
                .hostId(room.getHostId())
                .createdAt(room.getCreatedAt())
                .selectedRestaurantId(room.getSelectedRestaurantId())
                .location(room.getLocation())
                .locationAddress(room.getLocationAddress())
                .locationPlaceId(room.getLocationPlaceId())
                .latitude(room.getLatitude())
                .longitude(room.getLongitude())
                .status(room.getStatus())
                .members(memberResponses)
                .winningMenu(room.getWinningMenu())
                .matchedRestaurants(restaurantResponses)
                .defaultMenus(room.getCustomMenus())
                .totalMembers(room.getMembers().size())
                .completedMembersCount((int) completedCount)
                .voteStats(voteStats)
                .maxSwipeCount(room.getMaxSwipeCount())
                .build();
    }
}
