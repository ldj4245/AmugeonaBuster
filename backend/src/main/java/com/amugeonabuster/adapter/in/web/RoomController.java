package com.amugeonabuster.adapter.in.web;

import com.amugeonabuster.adapter.in.web.dto.CreateRoomRequest;
import com.amugeonabuster.adapter.in.web.dto.JoinRoomRequest;
import com.amugeonabuster.adapter.in.web.dto.RoomResponse;
import com.amugeonabuster.application.port.in.CreateRoomUseCase;
import com.amugeonabuster.application.port.in.CreateRoomUseCase.CreateRoomCommand;
import com.amugeonabuster.application.port.in.JoinRoomUseCase;
import com.amugeonabuster.application.port.in.JoinRoomUseCase.JoinRoomCommand;
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
}
