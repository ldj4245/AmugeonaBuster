package com.amugeonabuster.adapter.out.persistence;

import com.amugeonabuster.domain.model.Member;
import com.amugeonabuster.domain.model.Restaurant;
import com.amugeonabuster.domain.model.Room;
import com.amugeonabuster.domain.model.Swipe;
import org.springframework.stereotype.Component;

import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Collectors;

@Component
public class RoomMapper {
    private final ObjectMapper json = new ObjectMapper();

    /**
     * JPA Entity -> Pure Domain Model 변환
     */
    public Room toDomainModel(RoomJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        List<Member> domainMembers = jpaEntity.getMembers().stream()
                .map(memberEntity -> Member.builder()
                        .id(memberEntity.getId())
                        .nickname(memberEntity.getNickname())
                        .isReady(memberEntity.isReady())
                        .build())
                .collect(Collectors.toList());

        List<Swipe> domainSwipes = jpaEntity.getSwipes().stream()
                .map(swipeEntity -> Swipe.builder()
                        .memberId(swipeEntity.getMemberId())
                        .menuName(swipeEntity.getMenuName())
                        .isLike(swipeEntity.isLike())
                        .build())
                .collect(Collectors.toList());

        List<Restaurant> domainRestaurants = jpaEntity.getMatchedRestaurants().stream()
                .map(restaurantEntity -> Restaurant.builder()
                        .id(restaurantEntity.getId())
                        .name(restaurantEntity.getName())
                        .address(restaurantEntity.getAddress())
                        .latitude(restaurantEntity.getLatitude())
                        .longitude(restaurantEntity.getLongitude())
                        .phone(restaurantEntity.getPhone())
                        .category(restaurantEntity.getCategory())
                        .placeUrl(restaurantEntity.getPlaceUrl())
                        .externalPlaceId(restaurantEntity.getExternalPlaceId())
                        .distanceMeters(restaurantEntity.getDistanceMeters())
                        .matchType(restaurantEntity.getMatchType())
                        .build())
                .collect(Collectors.toList());

        List<String> domainCustomMenus = List.of();
        if (jpaEntity.getCustomMenus() != null && !jpaEntity.getCustomMenus().isEmpty()) {
            String savedMenus = jpaEntity.getCustomMenus();
            if (savedMenus.startsWith("[")) {
                try { domainCustomMenus = json.readValue(savedMenus, new TypeReference<List<String>>() {}); }
                catch (Exception e) { throw new IllegalStateException("저장된 메뉴를 읽을 수 없습니다.", e); }
            } else {
                domainCustomMenus = List.of(savedMenus.split(","));
            }
        }

        return Room.builder()
                .id(jpaEntity.getId())
                .hostId(jpaEntity.getHostId())
                .location(jpaEntity.getLocation())
                .locationAddress(jpaEntity.getLocationAddress())
                .locationPlaceId(jpaEntity.getLocationPlaceId())
                .latitude(jpaEntity.getLatitude())
                .longitude(jpaEntity.getLongitude())
                .status(jpaEntity.getStatus())
                .maxSwipeCount(jpaEntity.getMaxSwipeCount())
                .members(domainMembers)
                .swipes(domainSwipes)
                .winningMenu(jpaEntity.getWinningMenu())
                .matchedRestaurants(domainRestaurants)
                .customMenus(domainCustomMenus)
                .build();
    }

    /**
     * Pure Domain Model -> JPA Entity 변환 (Cascade를 위한 연관관계 설정 포함)
     */
    public RoomJpaEntity toJpaEntity(Room domainModel) {
        if (domainModel == null) {
            return null;
        }

        String customMenusStr = "";
        if (domainModel.getCustomMenus() != null && !domainModel.getCustomMenus().isEmpty()) {
            try { customMenusStr = json.writeValueAsString(domainModel.getCustomMenus()); }
            catch (Exception e) { throw new IllegalStateException("메뉴를 저장할 수 없습니다.", e); }
        }

        RoomJpaEntity jpaEntity = RoomJpaEntity.builder()
                .id(domainModel.getId())
                .hostId(domainModel.getHostId())
                .location(domainModel.getLocation())
                .locationAddress(domainModel.getLocationAddress())
                .locationPlaceId(domainModel.getLocationPlaceId())
                .latitude(domainModel.getLatitude())
                .longitude(domainModel.getLongitude())
                .status(domainModel.getStatus())
                .maxSwipeCount(domainModel.getMaxSwipeCount())
                .winningMenu(domainModel.getWinningMenu())
                .customMenus(customMenusStr)
                .build();

        // 도메인 내부에 격리된 멤버들을 양방향 JPA Entity 관계로 바인딩
        domainModel.getMembers().forEach(domainMember -> {
            MemberJpaEntity memberJpaEntity = MemberJpaEntity.builder()
                    .id(domainMember.getId())
                    .nickname(domainMember.getNickname())
                    .isReady(domainMember.isReady())
                    .build();
            jpaEntity.addMember(memberJpaEntity);
        });

        // 도메인 내부에 격리된 스와이프들을 양방향 JPA Entity 관계로 바인딩
        domainModel.getSwipes().forEach(domainSwipe -> {
            SwipeJpaEntity swipeJpaEntity = SwipeJpaEntity.builder()
                    .memberId(domainSwipe.getMemberId())
                    .menuName(domainSwipe.getMenuName())
                    .isLike(domainSwipe.isLike())
                    .build();
            jpaEntity.addSwipe(swipeJpaEntity);
        });

        // 도메인 내부에 격리된 식당들을 양방향 JPA Entity 관계로 바인딩
        domainModel.getMatchedRestaurants().forEach(domainRestaurant -> {
            RestaurantJpaEntity restaurantJpaEntity = RestaurantJpaEntity.builder()
                    .id(domainRestaurant.getId())
                    .name(domainRestaurant.getName())
                    .address(domainRestaurant.getAddress())
                    .latitude(domainRestaurant.getLatitude())
                    .longitude(domainRestaurant.getLongitude())
                    .phone(domainRestaurant.getPhone())
                    .category(domainRestaurant.getCategory())
                    .placeUrl(domainRestaurant.getPlaceUrl())
                    .externalPlaceId(domainRestaurant.getExternalPlaceId())
                    .distanceMeters(domainRestaurant.getDistanceMeters())
                    .matchType(domainRestaurant.getMatchType())
                    .build();
            jpaEntity.addRestaurant(restaurantJpaEntity);
        });

        return jpaEntity;
    }
}
