package com.amugeonabuster.domain.model;

import com.amugeonabuster.domain.exception.InvalidRoomStateException;
import com.amugeonabuster.domain.exception.MaxMemberExceededException;
import com.amugeonabuster.domain.exception.UnauthorizedHostException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomTest {

    private UUID hostId;
    private Room room;

    @BeforeEach
    void setUp() {
        hostId = UUID.randomUUID();
        room = Room.builder()
                .id("ROOM-7392")
                .hostId(hostId)
                .location("강남역")
                .build();
    }

    @Test
    @DisplayName("신규 유저가 대기방에 성공적으로 입장한다")
    void joinMember_success() {
        // given
        Member guest = Member.builder()
                .nickname("김스프")
                .build();

        // when
        room.joinMember(guest);

        // then
        assertThat(room.getMembers()).hasSize(1);
        assertThat(room.getMembers().get(0).getNickname()).isEqualTo("김스프");
    }

    @Test
    @DisplayName("대기방의 정원(10명)이 초과하면 가입이 차단되고 예외가 발생한다")
    void joinMember_limit_exceeded() {
        // given
        for (int i = 0; i < 10; i++) {
            room.joinMember(Member.builder().nickname("참가자" + i).build());
        }
        
        Member extraGuest = Member.builder().nickname("11번째참가자").build();

        // when & then
        assertThatThrownBy(() -> room.joinMember(extraGuest))
                .isInstanceOf(MaxMemberExceededException.class)
                .hasMessageContaining("방의 최대 정원은 10명입니다.");
    }

    @Test
    @DisplayName("방장이 게임(투표)을 활성화하면 방 상태가 PLAYING으로 전환된다")
    void startVoting_by_host_success() {
        // when
        room.startVoting(hostId);

        // then
        assertThat(room.getStatus()).isEqualTo(RoomStatus.PLAYING);
    }

    @Test
    @DisplayName("방장이 아닌 유저가 게임을 시작하려 하면 권한 에러가 발생한다")
    void startVoting_by_guest_fail() {
        // given
        UUID anonymousId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> room.startVoting(anonymousId))
                .isInstanceOf(UnauthorizedHostException.class)
                .hasMessageContaining("방장만 게임을 시작할 수 있습니다.");
    }

    @Test
    @DisplayName("이미 시작된 방이나 완료된 방에는 신규 가입이 불가능하다")
    void joinMember_in_active_room_fail() {
        // given
        room.startVoting(hostId);
        Member extraGuest = Member.builder().nickname("늦참러").build();

        // when & then
        assertThatThrownBy(() -> room.joinMember(extraGuest))
                .isInstanceOf(InvalidRoomStateException.class)
                .hasMessageContaining("이미 게임이 시작되었거나 종료된 방에는 참여할 수 없습니다.");
    }
}
