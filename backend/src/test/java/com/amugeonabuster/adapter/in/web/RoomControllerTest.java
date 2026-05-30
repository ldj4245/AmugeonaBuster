package com.amugeonabuster.adapter.in.web;

import com.amugeonabuster.adapter.in.web.dto.*;
import com.amugeonabuster.application.port.in.CreateRoomUseCase;
import com.amugeonabuster.application.port.in.CreateRoomUseCase.CreateRoomCommand;
import com.amugeonabuster.application.port.in.JoinRoomUseCase;
import com.amugeonabuster.application.port.in.JoinRoomUseCase.JoinRoomCommand;
import com.amugeonabuster.application.port.in.StartVotingUseCase;
import com.amugeonabuster.application.port.in.StartVotingUseCase.StartVotingCommand;
import com.amugeonabuster.application.port.in.SwipeMenuUseCase;
import com.amugeonabuster.application.port.in.SwipeMenuUseCase.SwipeMenuCommand;
import com.amugeonabuster.domain.model.Member;
import com.amugeonabuster.domain.model.Room;
import com.amugeonabuster.domain.model.RoomStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateRoomUseCase createRoomUseCase;

    @MockBean
    private JoinRoomUseCase joinRoomUseCase;

    @MockBean
    private StartVotingUseCase startVotingUseCase;

    @MockBean
    private SwipeMenuUseCase swipeMenuUseCase;

    @Test
    @DisplayName("방 개설 API 호출 시 HTTP 201 상태코드와 생성된 방 데이터를 JSON으로 응답받는다")
    void createRoom_api_success() throws Exception {
        // given
        CreateRoomRequest request = new CreateRoomRequest();
        request.setHostNickname("김방장");
        request.setLocation("강남역");

        UUID hostId = UUID.randomUUID();
        Room createdRoom = Room.builder()
                .id("ROOM-ABC123")
                .hostId(hostId)
                .location("강남역")
                .status(RoomStatus.LOBBY)
                .build();
        createdRoom.joinMember(Member.builder().id(hostId).nickname("김방장").isReady(true).build());

        when(createRoomUseCase.createRoom(any(CreateRoomCommand.class))).thenReturn(createdRoom);

        // when & then
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value("ROOM-ABC123"))
                .andExpect(jsonPath("$.location").value("강남역"))
                .andExpect(jsonPath("$.status").value("LOBBY"))
                .andExpect(jsonPath("$.members[0].nickname").value("김방장"))
                .andExpect(jsonPath("$.members[0].ready").value(true));
    }

    @Test
    @DisplayName("방 참여 API 호출 시 HTTP 200 상태코드와 갱신된 방 데이터를 JSON으로 응답받는다")
    void joinRoom_api_success() throws Exception {
        // given
        String roomId = "ROOM-XYZ999";
        JoinRoomRequest request = new JoinRoomRequest();
        request.setGuestNickname("이참가");

        UUID hostId = UUID.randomUUID();
        Room updatedRoom = Room.builder()
                .id(roomId)
                .hostId(hostId)
                .location("홍대입구")
                .status(RoomStatus.LOBBY)
                .build();
        updatedRoom.joinMember(Member.builder().id(hostId).nickname("김방장").isReady(true).build());
        updatedRoom.joinMember(Member.builder().nickname("이참가").isReady(false).build());

        when(joinRoomUseCase.joinRoom(any(JoinRoomCommand.class))).thenReturn(updatedRoom);

        // when & then
        mockMvc.perform(post("/api/rooms/{roomId}/members", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(roomId))
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.members[1].nickname").value("이참가"))
                .andExpect(jsonPath("$.members[1].ready").value(false));
    }

    @Test
    @DisplayName("방장의 투표 시작 API 호출 시 HTTP 200 상태코드와 PLAYING으로 전환된 방 데이터를 응답받는다")
    void startVoting_api_success() throws Exception {
        // given
        String roomId = "ROOM-ABC123";
        UUID hostId = UUID.randomUUID();
        StartVotingRequest request = new StartVotingRequest();
        request.setHostId(hostId);

        Room room = Room.builder()
                .id(roomId)
                .hostId(hostId)
                .location("강남역")
                .status(RoomStatus.PLAYING)
                .build();

        when(startVotingUseCase.startVoting(any(StartVotingCommand.class))).thenReturn(room);

        // when & then
        mockMvc.perform(post("/api/rooms/{roomId}/start", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(roomId))
                .andExpect(jsonPath("$.status").value("PLAYING"));
    }

    @Test
    @DisplayName("메뉴 스와이프 투표 API 호출 시 HTTP 200 상태코드와 투표 정보가 반영된 방 데이터를 응답받는다")
    void swipeMenu_api_success() throws Exception {
        // given
        String roomId = "ROOM-ABC123";
        UUID memberId = UUID.randomUUID();
        SwipeMenuRequest request = new SwipeMenuRequest();
        request.setMemberId(memberId);
        request.setMenuName("삼겹살");
        request.setLike(true);

        Room room = Room.builder()
                .id(roomId)
                .hostId(memberId)
                .location("강남역")
                .status(RoomStatus.LOBBY)
                .build();
        room.joinMember(Member.builder().id(memberId).nickname("투표자").isReady(true).build());
        room.startVoting(memberId);
        room.swipeMenu(memberId, "삼겹살", true);

        when(swipeMenuUseCase.swipeMenu(any(SwipeMenuCommand.class))).thenReturn(room);

        // when & then
        mockMvc.perform(post("/api/rooms/{roomId}/swipes", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(roomId))
                .andExpect(jsonPath("$.totalMembers").value(1))
                .andExpect(jsonPath("$.completedMembersCount").value(0)); // 15개 중 1개만 했으므로 완료수는 0
    }
}
