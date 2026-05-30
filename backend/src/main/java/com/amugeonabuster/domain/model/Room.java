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

@Getter
public class Room {
    private final String id;
    private final UUID hostId;
    private final String location;
    private RoomStatus status;
    private final List<Member> members;

    private static final int MAX_MEMBER_SIZE = 10;

    @Builder
    public Room(String id, UUID hostId, String location, RoomStatus status, List<Member> members) {
        this.id = id;
        this.hostId = hostId;
        this.location = location;
        this.status = status != null ? status : RoomStatus.LOBBY;
        this.members = members != null ? new ArrayList<>(members) : new ArrayList<>();
    }

    /**
     * 외부에서 리스트를 맘대로 조작할 수 없도록 읽기 전용으로 노출 (캡슐화 보장)
     */
    public List<Member> getMembers() {
        return Collections.unmodifiableList(members);
    }

    /**
     * 신규 멤버 가입 비즈니스 룰
     */
    public void joinMember(Member newMember) {
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
        if (!this.hostId.equals(requesterId)) {
            throw new UnauthorizedHostException("방장만 게임을 시작할 수 있습니다.");
        }
        if (this.status != RoomStatus.LOBBY) {
            throw new InvalidRoomStateException("대기방 상태에서만 게임을 시작할 수 있습니다.");
        }
        this.status = RoomStatus.PLAYING;
    }

    /**
     * 최종 투표 완료 및 게임 매칭 완료 룰
     */
    public void completeVoting() {
        if (this.status != RoomStatus.PLAYING) {
            throw new InvalidRoomStateException("투표 진행 중인 방만 매칭을 완료할 수 있습니다.");
        }
        this.status = RoomStatus.COMPLETED;
    }
}
