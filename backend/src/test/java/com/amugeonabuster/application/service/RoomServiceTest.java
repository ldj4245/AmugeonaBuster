package com.amugeonabuster.application.service;

import com.amugeonabuster.application.port.in.CreateRoomUseCase.CreateRoomCommand;
import com.amugeonabuster.application.port.in.JoinRoomUseCase.JoinRoomCommand;
import com.amugeonabuster.application.port.out.LoadRoomPort;
import com.amugeonabuster.application.port.out.SaveRoomPort;
import com.amugeonabuster.domain.model.Room;
import com.amugeonabuster.domain.model.RoomStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private SaveRoomPort saveRoomPort;

    @Mock
    private LoadRoomPort loadRoomPort;

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
}
