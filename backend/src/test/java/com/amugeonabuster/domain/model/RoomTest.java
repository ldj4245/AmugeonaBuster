package com.amugeonabuster.domain.model;

import com.amugeonabuster.domain.exception.InvalidRoomStateException;
import com.amugeonabuster.domain.exception.MaxMemberExceededException;
import com.amugeonabuster.domain.exception.UnauthorizedHostException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
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

    @Test
    @DisplayName("스와이프 투표 시 동일한 유저가 동일 메뉴에 중복 투표하면 멱등성에 따라 최초 요청만 유효하고 중복은 무시된다")
    void swipeMenu_idempotency_success() {
        // given
        UUID guestId = UUID.randomUUID();
        Member guest = Member.builder().id(guestId).nickname("투표자").build();
        room.joinMember(guest);
        room.startVoting(hostId);

        // when
        room.swipeMenu(guestId, "삼겹살", true); // 최초 투표 (Like)
        room.swipeMenu(guestId, "삼겹살", false); // 중복 투표 시도 (Dislike)

        // then
        assertThat(room.getSwipes()).hasSize(1);
        assertThat(room.getSwipes().get(0).isLike()).isTrue(); // 최초의 Like가 유지됨
    }

    @Test
    @DisplayName("모든 참여자가 전체 15개 카드를 스와이프 완료하기 전에는 isAllMembersCompletedSwiping이 false를 리턴한다")
    void isAllMembersCompletedSwiping_not_yet() {
        // given
        UUID guestId = UUID.randomUUID();
        Member guest = Member.builder().id(guestId).nickname("투표자").build();
        room.joinMember(guest);
        room.startVoting(hostId);

        // when (15개 중 14개만 스와이프)
        for (int i = 0; i < 14; i++) {
            room.swipeMenu(guestId, DefaultMenus.MENUS.get(i), true);
        }

        // then
        assertThat(room.isAllMembersCompletedSwiping()).isFalse();
    }

    @Test
    @DisplayName("모든 참여자가 15개 카드를 전부 스와이프하면 isAllMembersCompletedSwiping이 true를 리턴한다")
    void isAllMembersCompletedSwiping_completed() {
        // given
        UUID guestId = UUID.randomUUID();
        Member guest = Member.builder().id(guestId).nickname("투표자").build();
        room.joinMember(guest);
        room.startVoting(hostId);

        // when (15개 전체 스와이프)
        for (int i = 0; i < 15; i++) {
            room.swipeMenu(guestId, DefaultMenus.MENUS.get(i), true);
        }

        // then
        assertThat(room.isAllMembersCompletedSwiping()).isTrue();
    }

    @Test
    @DisplayName("일반적인 선호도 집계(F-401)와 거부권 필터링(F-402)을 통해 1위 메뉴가 정상 선정된다")
    void determineWinningMenu_normal_consensus() {
        // given
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        room.joinMember(Member.builder().id(user1).nickname("유저1").build());
        room.joinMember(Member.builder().id(user2).nickname("유저2").build());
        room.startVoting(hostId);

        // 삼겹살: 유저1 Like(+1), 유저2 Like(+1) => 총 +2점 (비토 없음)
        room.swipeMenu(user1, "삼겹살", true);
        room.swipeMenu(user2, "삼겹살", true);

        // 떡볶이: 유저1 Like(+1), 유저2 Dislike(-2) => 총 -1점 (비토 있음 - 제외)
        room.swipeMenu(user1, "떡볶이", true);
        room.swipeMenu(user2, "떡볶이", false);

        // 김치찌개: 유저1 Like(+1), 유저2 무투표 => 총 +1점 (비토 없음)
        room.swipeMenu(user1, "김치찌개", true);

        // when
        room.determineWinningMenu();

        // then
        assertThat(room.getStatus()).isEqualTo(RoomStatus.COMPLETED);
        assertThat(room.getWinningMenu()).isEqualTo("삼겹살"); // 가장 높은 점수이자 비토 없는 삼겹살 낙점
    }

    @Test
    @DisplayName("모든 메뉴가 거부권을 당한 극한의 상황에서는 비토 폭탄 구제 룰(F-403)이 활성화되어 1위를 산출한다")
    void determineWinningMenu_veto_fallback() {
        // given
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        room.joinMember(Member.builder().id(user1).nickname("유저1").build());
        room.joinMember(Member.builder().id(user2).nickname("유저2").build());
        room.startVoting(hostId);

        // 모든 15가지 메뉴에 유저2가 Dislike를 표출하여 전부 비토로 전멸시킴
        for (String menu : DefaultMenus.MENUS) {
            room.swipeMenu(user2, menu, false);
        }

        // 삼겹살: 유저1 Like(+1), 유저2 Dislike => 구제 점수: 1 * 1.0 - 1 * 0.5 = 0.5
        room.swipeMenu(user1, "삼겹살", true);

        // 떡볶이: 유저1 Dislike, 유저2 Dislike => 구제 점수: 0 * 1.0 - 2 * 0.5 = -1.0
        room.swipeMenu(user1, "떡볶이", false);

        // when
        room.determineWinningMenu();

        // then
        assertThat(room.getStatus()).isEqualTo(RoomStatus.COMPLETED);
        assertThat(room.getWinningMenu()).isEqualTo("삼겹살"); // 구제 공식 최고점인 삼겹살 낙점
    }

    @Test
    @DisplayName("합의 투표 결과가 동점일 경우 타이브레이커에 의해 기본 인덱스가 더 빠른 선순위 메뉴가 당선된다")
    void determineWinningMenu_tie_breaker() {
        // given
        UUID user1 = UUID.randomUUID();
        room.joinMember(Member.builder().id(user1).nickname("유저1").build());
        room.startVoting(hostId);

        // 삼겹살(인덱스 0): Like 1개 (+1점)
        room.swipeMenu(user1, "삼겹살", true);

        // 김치찌개(인덱스 1): Like 1개 (+1점)
        room.swipeMenu(user1, "김치찌개", true);

        // when
        room.determineWinningMenu();

        // then
        assertThat(room.getWinningMenu()).isEqualTo("삼겹살"); // 점수와 Like 수 동점 시 인덱스가 더 앞선 삼겹살 당선
    }
}
