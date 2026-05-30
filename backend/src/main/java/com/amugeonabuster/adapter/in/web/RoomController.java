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
import com.amugeonabuster.domain.model.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final CreateRoomUseCase createRoomUseCase;
    private final JoinRoomUseCase joinRoomUseCase;
    private final StartVotingUseCase startVotingUseCase;
    private final SwipeMenuUseCase swipeMenuUseCase;

    /**
     * 리소스 지향 방 개설 API (POST /api/rooms)
     */
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@RequestBody CreateRoomRequest request) {
        CreateRoomCommand command = new CreateRoomCommand(
                request.getHostNickname(),
                request.getLocation()
        );

        Room room = createRoomUseCase.createRoom(command);
        RoomResponse response = RoomResponse.fromDomain(room);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 리소스 지향 대기방 참가 API (POST /api/rooms/{roomId}/members)
     */
    @PostMapping("/{roomId}/members")
    public ResponseEntity<RoomResponse> joinRoom(
            @PathVariable("roomId") String roomId,
            @RequestBody JoinRoomRequest request
    ) {
        JoinRoomCommand command = new JoinRoomCommand(
                roomId,
                request.getGuestNickname()
        );

        Room room = joinRoomUseCase.joinRoom(command);
        RoomResponse response = RoomResponse.fromDomain(room);

        return ResponseEntity.ok(response);
    }

    /**
     * 방장의 실시간 투표 시작 API (POST /api/rooms/{roomId}/start)
     */
    @PostMapping("/{roomId}/start")
    public ResponseEntity<RoomResponse> startVoting(
            @PathVariable("roomId") String roomId,
            @RequestBody StartVotingRequest request
    ) {
        StartVotingCommand command = new StartVotingCommand(roomId, request.getHostId());
        Room room = startVotingUseCase.startVoting(command);
        RoomResponse response = RoomResponse.fromDomain(room);

        return ResponseEntity.ok(response);
    }

    /**
     * 리소스 지향 스와이프 투표 API (POST /api/rooms/{roomId}/swipes)
     */
    @PostMapping("/{roomId}/swipes")
    public ResponseEntity<RoomResponse> swipeMenu(
            @PathVariable("roomId") String roomId,
            @RequestBody SwipeMenuRequest request
    ) {
        SwipeMenuCommand command = new SwipeMenuCommand(
                roomId,
                request.getMemberId(),
                request.getMenuName(),
                request.isLike()
        );

        Room room = swipeMenuUseCase.swipeMenu(command);
        RoomResponse response = RoomResponse.fromDomain(room);

        return ResponseEntity.ok(response);
    }
}
