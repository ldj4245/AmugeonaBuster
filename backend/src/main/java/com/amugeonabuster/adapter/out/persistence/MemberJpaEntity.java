package com.amugeonabuster.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private boolean isReady;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private RoomJpaEntity room;

    @Builder
    public MemberJpaEntity(UUID id, String nickname, boolean isReady) {
        this.id = id;
        this.nickname = nickname;
        this.isReady = isReady;
    }

    /**
     * 연관관계 편의 메서드 (방 매핑 전용)
     */
    public void associateRoom(RoomJpaEntity room) {
        this.room = room;
    }
}
