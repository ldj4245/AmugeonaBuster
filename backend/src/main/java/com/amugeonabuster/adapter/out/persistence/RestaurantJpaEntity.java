package com.amugeonabuster.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "matched_restaurants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestaurantJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private RoomJpaEntity room;

    @Builder
    public RestaurantJpaEntity(UUID id, String name, String address, double latitude, double longitude, String phone) {
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
    }

    /**
     * 연관관계 편의 메서드
     */
    public void associateRoom(RoomJpaEntity room) {
        this.room = room;
    }
}
