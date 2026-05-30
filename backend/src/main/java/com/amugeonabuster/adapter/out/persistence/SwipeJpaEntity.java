package com.amugeonabuster.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "swipes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SwipeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private String menuName;

    @Column(nullable = false)
    private boolean isLike;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private RoomJpaEntity room;

    @Builder
    public SwipeJpaEntity(Long id, UUID memberId, String menuName, boolean isLike) {
        this.id = id;
        this.memberId = memberId;
        this.menuName = menuName;
        this.isLike = isLike;
    }

    /**
     * 연관관계 편의 메서드
     */
    public void associateRoom(RoomJpaEntity room) {
        this.room = room;
    }
}
