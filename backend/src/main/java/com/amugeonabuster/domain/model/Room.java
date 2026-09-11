package com.amugeonabuster.domain.model;

import com.amugeonabuster.domain.exception.InvalidRoomStateException;
import com.amugeonabuster.domain.exception.MaxMemberExceededException;
import com.amugeonabuster.domain.exception.UnauthorizedHostException;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class Room {
    private final String id;
    private UUID hostId;
    private java.time.Instant createdAt;
    private UUID selectedRestaurantId;
    private final String location;
    private final String locationAddress;
    private final String locationPlaceId;
    private final Double latitude;
    private final Double longitude;
    private RoomStatus status;
    private final List<Member> members;
    private final List<Swipe> swipes;
    private String winningMenu;
    private final List<Restaurant> matchedRestaurants;
    private final int maxSwipeCount;
    private final List<String> customMenus;

    private static final int MAX_MEMBER_SIZE = 10;

    @Builder
    public Room(String id, UUID hostId, String location, String locationAddress, String locationPlaceId,
            Double latitude, Double longitude, RoomStatus status, List<Member> members, List<Swipe> swipes,
            String winningMenu, List<Restaurant> matchedRestaurants, int maxSwipeCount, List<String> customMenus, java.time.Instant createdAt, UUID selectedRestaurantId) {
        this.createdAt = createdAt;
        this.selectedRestaurantId = selectedRestaurantId;
        this.id = id;
        this.hostId = hostId;
        this.location = location;
        this.locationAddress = locationAddress != null ? locationAddress : "";
        this.locationPlaceId = locationPlaceId != null ? locationPlaceId : "";
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status != null ? status : RoomStatus.LOBBY;
        this.members = members != null ? new ArrayList<>(members) : new ArrayList<>();
        this.swipes = swipes != null ? new ArrayList<>(swipes) : new ArrayList<>();
        this.winningMenu = winningMenu;
        this.matchedRestaurants = matchedRestaurants != null ? new ArrayList<>(matchedRestaurants) : new ArrayList<>();
        if (customMenus != null && !customMenus.isEmpty()) {
            if (customMenus.size() > 30 || customMenus.stream().anyMatch(m -> m == null || m.isBlank() || m.length() > 30)
                    || customMenus.stream().distinct().count() != customMenus.size()) {
                throw new IllegalArgumentException("메뉴는 중복 없이 30자 이하로 최대 30개까지 선택해 주세요.");
            }
            this.customMenus = new ArrayList<>(customMenus);
            this.maxSwipeCount = this.customMenus.size();
        } else {
            int limit = maxSwipeCount != 0 ? maxSwipeCount : 15;
            this.customMenus = new ArrayList<>(DefaultMenus.MENUS.subList(0, limit));
            this.maxSwipeCount = limit;
        }
    }

    public boolean hasLocationCoordinates() {
        return latitude != null && longitude != null
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180
                && (latitude != 0 || longitude != 0);
    }

    /**
     * 외부에서 리스트를 맘대로 조작할 수 없도록 읽기 전용으로 노출 (캡슐화 보장)
     */
    public List<Member> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public List<Swipe> getSwipes() {
        return Collections.unmodifiableList(swipes);
    }

    public List<Restaurant> getMatchedRestaurants() {
        return Collections.unmodifiableList(matchedRestaurants);
    }

    public List<String> getCustomMenus() {
        return Collections.unmodifiableList(customMenus);
    }

    /**
     * 신규 멤버 가입 비즈니스 룰
     */
    public boolean isExpired() {
        return createdAt != null && createdAt.isBefore(java.time.Instant.now().minusSeconds(10800));
    }

    public void leave(UUID memberId) {
        members.removeIf(m -> m.getId().equals(memberId));
        swipes.removeIf(s -> s.getMemberId().equals(memberId));
        if (memberId.equals(hostId) && !members.isEmpty()) hostId = members.get(0).getId();
    }

    public void selectRestaurant(UUID actor, UUID restaurantId) {
        if (!hostId.equals(actor)) throw new IllegalArgumentException("방장만 식당을 확정할 수 있습니다.");
        if (status != RoomStatus.COMPLETED || matchedRestaurants.stream().noneMatch(r -> r.getId().equals(restaurantId)))
            throw new IllegalArgumentException("결과에 있는 식당을 선택해 주세요.");
        selectedRestaurantId = restaurantId;
    }

    public void joinMember(Member newMember) {
        if (isExpired()) throw new IllegalArgumentException("만료된 방입니다. 새 방을 만들어 주세요.");
        if (this.status != RoomStatus.LOBBY) {
            throw new InvalidRoomStateException("이미 게임이 시작되었거나 종료된 방에는 참여할 수 없습니다.");
        }
        if (this.members.size() >= MAX_MEMBER_SIZE) {
            throw new MaxMemberExceededException("방의 최대 정원은 " + MAX_MEMBER_SIZE + "명입니다.");
        }
        
        // 중복 가입 체크
        boolean isDuplicate = this.members.stream()
                .anyMatch(m -> m.getId().equals(newMember.getId()));
        
        if (!isDuplicate) {
            this.members.add(newMember);
        }
    }

    /**
     * 방장의 게임(투표) 시작 비즈니스 룰
     */
    public void startVoting(UUID requesterId) {
        if (isExpired()) throw new IllegalArgumentException("만료된 방입니다.");
        if (!this.hostId.equals(requesterId)) {
            throw new UnauthorizedHostException("방장만 게임을 시작할 수 있습니다.");
        }
        if (this.status != RoomStatus.LOBBY) {
            throw new InvalidRoomStateException("대기방 상태에서만 게임을 시작할 수 있습니다.");
        }
        this.status = RoomStatus.PLAYING;
    }

    /**
     * 메뉴 스와이프 등록 비즈니스 룰 (멱등성 보장)
     */
    public void swipeMenu(UUID memberId, String menuName, boolean isLike) {
        if (isExpired()) throw new IllegalArgumentException("만료된 방입니다.");
        if (!this.customMenus.contains(menuName)) {
            throw new IllegalArgumentException("이 방의 후보 메뉴만 선택할 수 있습니다.");
        }
        if (this.status != RoomStatus.PLAYING) {
            throw new InvalidRoomStateException("투표가 진행 중인 방에서만 스와이프할 수 있습니다.");
        }

        // 멤버 존재 검증
        boolean memberExists = this.members.stream()
                .anyMatch(m -> m.getId().equals(memberId));
        if (!memberExists) {
            throw new IllegalArgumentException("방에 속해있지 않은 멤버의 요청입니다.");
        }

        // 멱등성 보장: 동일 유저가 동일 메뉴에 이미 투표했는지 체크하여 중복 요청 무시
        boolean isDuplicated = this.swipes.stream()
                .anyMatch(s -> s.getMemberId().equals(memberId) && s.getMenuName().equals(menuName));

        if (!isDuplicated) {
            this.swipes.add(Swipe.builder()
                    .memberId(memberId)
                    .menuName(menuName)
                    .isLike(isLike)
                    .build());
        }
    }

    /**
     * 모든 참여자가 전체 15개 카드를 모두 스와이프했는지 판별
     */
    public boolean isAllMembersCompletedSwiping() {
        if (this.members.isEmpty()) {
            return false;
        }

        int targetMenuCount = this.maxSwipeCount;

        for (Member m : this.members) {
            long uniqueSwipedCount = this.swipes.stream()
                    .filter(s -> s.getMemberId().equals(m.getId()))
                    .map(Swipe::getMenuName)
                    .distinct()
                    .count();

            if (uniqueSwipedCount < targetMenuCount) {
                return false;
            }
        }
        return true;
    }

    /**
     * 합의점(Consensus) 도출 매칭 알고리즘 가동
     */
    public void determineWinningMenu() {
        if (this.status != RoomStatus.PLAYING) {
            throw new InvalidRoomStateException("투표 진행 중인 방만 최종 매칭을 완료할 수 있습니다.");
        }

        List<String> allMenus = this.customMenus;
        List<MenuScore> scores = new ArrayList<>();

        for (int i = 0; i < allMenus.size(); i++) {
            String menu = allMenus.get(i);
            long likes = this.swipes.stream()
                    .filter(s -> s.getMenuName().equals(menu) && s.isLike())
                    .count();
            long dislikes = this.swipes.stream()
                    .filter(s -> s.getMenuName().equals(menu) && !s.isLike())
                    .count();

            long regularScore = likes * 1 + dislikes * (-2);
            double fallbackScore = likes * 1.0 - dislikes * 0.5;
            boolean isVetoed = dislikes > 0;

            if (likes + dislikes > 0) scores.add(new MenuScore(menu, i, likes, dislikes, regularScore, fallbackScore, isVetoed));
        }

        // F-402 거부권 필터링 시도
        List<MenuScore> nonVetoedScores = scores.stream()
                .filter(s -> !s.isVetoed)
                .collect(Collectors.toList());

        MenuScore winner;
        if (!nonVetoedScores.isEmpty()) {
            // 거부권 없는 메뉴가 있는 경우 정규 알고리즘 작동
            winner = nonVetoedScores.stream()
                    .max((s1, s2) -> {
                        if (s1.regularScore != s2.regularScore) {
                            return Long.compare(s1.regularScore, s2.regularScore);
                        }
                        if (s1.likes != s2.likes) {
                            return Long.compare(s1.likes, s2.likes);
                        }
                        // 동점인 경우 기본 인덱스가 더 앞서 있는 것 (인덱스 값이 더 작은 것) 선호
                        return Integer.compare(s2.defaultIndex, s1.defaultIndex);
                    })
                    .orElseThrow();
        } else {
            // F-403 비토 폭탄 구제 알고리즘 작동
            winner = scores.stream()
                    .max((s1, s2) -> {
                        if (Double.compare(s1.fallbackScore, s2.fallbackScore) != 0) {
                            return Double.compare(s1.fallbackScore, s2.fallbackScore);
                        }
                        if (s1.likes != s2.likes) {
                            return Long.compare(s1.likes, s2.likes);
                        }
                        return Integer.compare(s2.defaultIndex, s1.defaultIndex);
                    })
                    .orElseThrow();
        }

        this.winningMenu = winner.menuName;
        this.status = RoomStatus.COMPLETED;
    }

    public void associateMatchedRestaurants(List<Restaurant> restaurants) {
        this.matchedRestaurants.clear();
        if (restaurants != null) {
            this.matchedRestaurants.addAll(restaurants);
        }
    }

    /**
     * 알고리즘 연산을 위한 헬퍼 내부 레코드 구조
     */
    private static class MenuScore {
        final String menuName;
        final int defaultIndex;
        final long likes;
        final long dislikes;
        final long regularScore;
        final double fallbackScore;
        final boolean isVetoed;

        MenuScore(String menuName, int defaultIndex, long likes, long dislikes, long regularScore, double fallbackScore, boolean isVetoed) {
            this.menuName = menuName;
            this.defaultIndex = defaultIndex;
            this.likes = likes;
            this.dislikes = dislikes;
            this.regularScore = regularScore;
            this.fallbackScore = fallbackScore;
            this.isVetoed = isVetoed;
        }
    }
}
