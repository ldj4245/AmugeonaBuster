package com.amugeonabuster.application.service;

import com.amugeonabuster.application.port.in.CreateRoomUseCase.CreateRoomCommand;
import com.amugeonabuster.application.port.in.JoinRoomUseCase.JoinRoomCommand;
import com.amugeonabuster.application.port.in.StartVotingUseCase.StartVotingCommand;
import com.amugeonabuster.application.port.in.SwipeMenuUseCase.SwipeMenuCommand;
import com.amugeonabuster.application.port.out.BroadcastRoomStatePort;
import com.amugeonabuster.application.port.out.LoadRoomPort;
import com.amugeonabuster.application.port.out.RecommendRestaurantsPort;
import com.amugeonabuster.application.port.out.SaveRoomPort;
import com.amugeonabuster.domain.model.DefaultMenus;
import com.amugeonabuster.domain.model.Member;
import com.amugeonabuster.domain.model.Restaurant;
import com.amugeonabuster.domain.model.Room;
import com.amugeonabuster.domain.model.RoomStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private SaveRoomPort saveRoomPort;

    @Mock
    private LoadRoomPort loadRoomPort;

    @Mock
    private BroadcastRoomStatePort broadcastRoomStatePort;

    @Mock
    private RecommendRestaurantsPort recommendRestaurantsPort;

    @InjectMocks
    private RoomService roomService;

    @Test
    @DisplayName("방장 닉네임과 약속 장소를 받아 방을 성공적으로 개설하고 저장한다")
    void createRoom_success() {
        // given
        CreateRoomCommand command = new CreateRoomCommand("김방장", "강남역");

        // when
        Room room = roomService.createRoom(command);

        // then
        assertThat(room).isNotNull();
        assertThat(room.getId()).startsWith("ROOM-");
        assertThat(room.getLocation()).isEqualTo("강남역");
        assertThat(room.getMembers()).hasSize(1);
        assertThat(room.getMembers().get(0).getNickname()).isEqualTo("김방장");
        assertThat(room.getMembers().get(0).isReady()).isTrue();

        verify(saveRoomPort, times(1)).saveRoom(any(Room.class));
    }

    @Test
    @DisplayName("유효한 방 코드로 다른 참여자가 대기방에 성공적으로 합류한다")
    void joinRoom_success() {
        // given
        String roomId = "ROOM-ABC123";
        Room existingRoom = Room.builder()
                .id(roomId)
                .hostId(UUID.randomUUID())
                .location("홍대입구")
                .status(RoomStatus.LOBBY)
                .build();

        when(loadRoomPort.loadRoom(roomId)).thenReturn(Optional.of(existingRoom));
        JoinRoomCommand command = new JoinRoomCommand(roomId, "이참가");

        // when
        Room room = roomService.joinRoom(command);

        // then
        assertThat(room).isNotNull();
        assertThat(room.getMembers()).hasSize(1);
        assertThat(room.getMembers().get(0).getNickname()).isEqualTo("이참가");
        assertThat(room.getMembers().get(0).isReady()).isFalse();

        verify(loadRoomPort, times(1)).loadRoom(roomId);
        verify(saveRoomPort, times(1)).saveRoom(existingRoom);
        verify(broadcastRoomStatePort, times(1)).broadcastRoomState(existingRoom);
    }

    @Test
    @DisplayName("존재하지 않는 방 코드로 입장 시도 시 에러가 발생한다")
    void joinRoom_invalid_room_code_fail() {
        // given
        String invalidRoomId = "ROOM-XYZ999";
        when(loadRoomPort.loadRoom(invalidRoomId)).thenReturn(Optional.empty());
        JoinRoomCommand command = new JoinRoomCommand(invalidRoomId, "불청객");

        // when & then
        assertThatThrownBy(() -> roomService.joinRoom(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 방입니다.");

        verify(saveRoomPort, never()).saveRoom(any(Room.class));
    }

    @Test
    @DisplayName("방장의 유효한 게임(투표) 시작 명령 시 상태가 전환되고 영속화 및 소켓 브로드캐스트가 호출된다")
    void startVoting_success() {
        // given
        String roomId = "ROOM-123456";
        UUID hostId = UUID.randomUUID();
        Room existingRoom = Room.builder()
                .id(roomId)
                .hostId(hostId)
                .location("강남역")
                .status(RoomStatus.LOBBY)
                .build();

        when(loadRoomPort.loadRoom(roomId)).thenReturn(Optional.of(existingRoom));
        StartVotingCommand command = new StartVotingCommand(roomId, hostId);

        // when
        Room room = roomService.startVoting(command);

        // then
        assertThat(room.getStatus()).isEqualTo(RoomStatus.PLAYING);
        verify(loadRoomPort, times(1)).loadRoom(roomId);
        verify(saveRoomPort, times(1)).saveRoom(existingRoom);
        verify(broadcastRoomStatePort, times(1)).broadcastRoomState(existingRoom);
    }

    @Test
    @DisplayName("스와이프 투표 제출 시 도메인 로직이 호출되고 영속화 및 소켓 브로드캐스트가 작동한다")
    void swipeMenu_normal_vote() {
        // given
        String roomId = "ROOM-123456";
        UUID memberId = UUID.randomUUID();
        Room existingRoom = Room.builder()
                .id(roomId)
                .hostId(memberId)
                .location("강남역")
                .status(RoomStatus.PLAYING)
                .members(List.of(Member.builder().id(memberId).nickname("투표자").build()))
                .build();

        when(loadRoomPort.loadRoom(roomId)).thenReturn(Optional.of(existingRoom));
        SwipeMenuCommand command = new SwipeMenuCommand(roomId, memberId, "삼겹살", true);

        // when
        Room room = roomService.swipeMenu(command);

        // then
        assertThat(room.getSwipes()).hasSize(1);
        assertThat(room.getSwipes().get(0).getMenuName()).isEqualTo("삼겹살");
        assertThat(room.getSwipes().get(0).isLike()).isTrue();

        verify(loadRoomPort, times(1)).loadRoom(roomId);
        verify(saveRoomPort, times(1)).saveRoom(existingRoom);
        verify(broadcastRoomStatePort, times(1)).broadcastRoomState(existingRoom);
        verifyNoInteractions(recommendRestaurantsPort); // 전원 완료 전이므로 호출되지 않음
    }

    @Test
    @DisplayName("마지막 참여자가 스와이프를 완료하면 합의 매칭(Determine)이 돌고 맛집 추천 아웃고잉 포트가 작동한다")
    void swipeMenu_completed_game_trigger_recommendations() {
        // given
        String roomId = "ROOM-123456";
        UUID memberId = UUID.randomUUID();
        Room existingRoom = Room.builder()
                .id(roomId)
                .hostId(memberId)
                .location("강남역")
                .status(RoomStatus.PLAYING)
                .members(List.of(Member.builder().id(memberId).nickname("투표자").build()))
                .build();

        // 15개 중 14개 미투표된 상태에서 마지막 한 장을 스와이프 제출하는 상황 재현
        // room에 이미 14개 스와이프를 주입
        for (int i = 0; i < 14; i++) {
            existingRoom.swipeMenu(memberId, DefaultMenus.MENUS.get(i), true);
        }

        when(loadRoomPort.loadRoom(roomId)).thenReturn(Optional.of(existingRoom));
        SwipeMenuCommand command = new SwipeMenuCommand(roomId, memberId, DefaultMenus.MENUS.get(14), true);

        List<Restaurant> mockRecommend = List.of(Restaurant.builder().name("명가 삼겹살 본점").address("강남구").build());
        when(recommendRestaurantsPort.recommend(any(), eq("강남역"))).thenReturn(mockRecommend);

        // when
        Room room = roomService.swipeMenu(command);

        // then
        assertThat(room.getStatus()).isEqualTo(RoomStatus.COMPLETED);
        assertThat(room.getWinningMenu()).isNotNull();
        assertThat(room.getMatchedRestaurants()).hasSize(1);

        verify(loadRoomPort, times(1)).loadRoom(roomId);
        verify(saveRoomPort, times(1)).saveRoom(existingRoom);
        verify(recommendRestaurantsPort, times(1)).recommend(any(), eq("강남역"));
        verify(broadcastRoomStatePort, times(1)).broadcastRoomState(existingRoom);
    }
}
