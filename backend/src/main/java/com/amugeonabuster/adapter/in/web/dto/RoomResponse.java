package com.amugeonabuster.adapter.in.web.dto;

import com.amugeonabuster.domain.model.DefaultMenus;
import com.amugeonabuster.domain.model.Room;
import com.amugeonabuster.domain.model.RoomStatus;
import com.amugeonabuster.domain.model.Swipe;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
public class RoomResponse {
    private final String roomId;
    private final UUID hostId;
    private final String location;
    private final RoomStatus status;
    private final List<MemberResponse> members;
    private final String winningMenu;
    private final List<RestaurantResponse> matchedRestaurants;
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
     * 도메인 객체로부터 API 응답 포맷으로 고속 변환
     */
    public static RoomResponse fromDomain(Room room) {
        if (room == null) {
            return null;
        }

        List<MemberResponse> memberResponses = room.getMembers().stream()
                .map(m -> MemberResponse.builder()
                        .id(m.getId())
                        .nickname(m.getNickname())
                        .isReady(m.isReady())
                        .build())
                .collect(Collectors.toList());

        List<RestaurantResponse> restaurantResponses = room.getMatchedRestaurants().stream()
                .map(RestaurantResponse::fromDomain)
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

        // 메뉴별 투표 통계 집계 (좋아요/싫어요) - 활성화된 서브리스트만 집계
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

        return RoomResponse.builder()
                .roomId(room.getId())
                .hostId(room.getHostId())
                .location(room.getLocation())
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
