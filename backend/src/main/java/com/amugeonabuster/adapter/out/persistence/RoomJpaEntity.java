package com.amugeonabuster.adapter.out.persistence;

import com.amugeonabuster.domain.model.RoomStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private UUID hostId;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status;

    @Column(nullable = false)
    private int maxSwipeCount;

    @Column(nullable = false, length = 1000)
    private String customMenus;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberJpaEntity> members = new ArrayList<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SwipeJpaEntity> swipes = new ArrayList<>();

    private String winningMenu;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RestaurantJpaEntity> matchedRestaurants = new ArrayList<>();

    @Builder
    public RoomJpaEntity(String id, UUID hostId, String location, RoomStatus status, int maxSwipeCount, List<MemberJpaEntity> members, List<SwipeJpaEntity> swipes, String winningMenu, List<RestaurantJpaEntity> matchedRestaurants, String customMenus) {
        this.id = id;
        this.hostId = hostId;
        this.location = location;
        this.status = status;
        this.maxSwipeCount = maxSwipeCount != 0 ? maxSwipeCount : 15;
        this.members = members != null ? members : new ArrayList<>();
        this.swipes = swipes != null ? swipes : new ArrayList<>();
        this.winningMenu = winningMenu;
        this.matchedRestaurants = matchedRestaurants != null ? matchedRestaurants : new ArrayList<>();
        this.customMenus = customMenus != null ? customMenus : "";
    }

    /**
     * 양방향 연관관계 동기화 편의 메서드
     */
    public void addMember(MemberJpaEntity member) {
        this.members.add(member);
        member.associateRoom(this);
    }

    public void addSwipe(SwipeJpaEntity swipe) {
        this.swipes.add(swipe);
        swipe.associateRoom(this);
    }

    public void addRestaurant(RestaurantJpaEntity restaurant) {
        this.matchedRestaurants.add(restaurant);
        restaurant.associateRoom(this);
    }
}
