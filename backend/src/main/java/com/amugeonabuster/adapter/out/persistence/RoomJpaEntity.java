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

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberJpaEntity> members = new ArrayList<>();

    @Builder
    public RoomJpaEntity(String id, UUID hostId, String location, RoomStatus status, List<MemberJpaEntity> members) {
        this.id = id;
        this.hostId = hostId;
        this.location = location;
        this.status = status;
        this.members = members != null ? members : new ArrayList<>();
    }

    /**
     * 양방향 연관관계 동기화 편의 메서드
     */
    public void addMember(MemberJpaEntity member) {
        this.members.add(member);
        member.associateRoom(this);
    }
}
