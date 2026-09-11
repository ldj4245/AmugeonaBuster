package com.amugeonabuster.adapter.in.web;

import com.amugeonabuster.adapter.in.web.dto.*;
import com.amugeonabuster.application.port.in.CreateRoomUseCase;
import com.amugeonabuster.application.port.in.CreateRoomUseCase.CreateRoomCommand;
import com.amugeonabuster.application.port.in.GetRoomUseCase;
import com.amugeonabuster.application.port.in.JoinRoomUseCase;
import com.amugeonabuster.application.port.in.JoinRoomUseCase.JoinRoomCommand;
import com.amugeonabuster.application.port.in.StartVotingUseCase;
import com.amugeonabuster.application.port.in.StartVotingUseCase.StartVotingCommand;
import com.amugeonabuster.application.port.in.SwipeMenuUseCase;
import com.amugeonabuster.application.port.in.SwipeMenuUseCase.SwipeMenuCommand;
import com.amugeonabuster.domain.model.Room;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final CreateRoomUseCase createRoomUseCase;
    private final GetRoomUseCase getRoomUseCase;
    private final JoinRoomUseCase joinRoomUseCase;
    private final StartVotingUseCase startVotingUseCase;
    private final SwipeMenuUseCase swipeMenuUseCase;

    /**
     * 리소스 지향 방 개설 API (POST /api/rooms)
     */
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@RequestBody CreateRoomRequest request, HttpSession session) {
        CreateRoomCommand command = new CreateRoomCommand(
                request.getHostNickname(),
                request.getLocation(),
                request.getCustomMenus(),
                request.getLocationAddress(),
                request.getLocationPlaceId(),
                request.getLatitude(),
                request.getLongitude()
        );

        Room room = createRoomUseCase.createRoom(command);
        session.setAttribute("room:" + room.getId(), room.getHostId());
        RoomResponse response = RoomResponse.fromDomain(room);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 리소스 지향 대기방 참가 API (POST /api/rooms/{roomId}/members)
     */
    @PostMapping("/{roomId}/members")
    public ResponseEntity<RoomResponse> joinRoom(
            @PathVariable("roomId") String roomId,
            @RequestBody JoinRoomRequest request, HttpSession session
    ) {
        Object existing = session.getAttribute("room:" + roomId);
        var current = getRoomUseCase.getRoom(roomId);
        if (existing != null && current.isPresent() && current.get().getMembers().stream().anyMatch(m -> m.getId().equals(existing)))
            return ResponseEntity.ok(RoomResponse.fromDomain(current.get()));
        JoinRoomCommand command = new JoinRoomCommand(
                roomId,
                request.getGuestNickname()
        );

        Room room = joinRoomUseCase.joinRoom(command);
        session.setAttribute("room:" + roomId, room.getMembers().get(room.getMembers().size() - 1).getId());
        RoomResponse response = RoomResponse.fromDomain(room);

        return ResponseEntity.ok(response);
    }

    /**
     * 방장의 실시간 투표 시작 API (POST /api/rooms/{roomId}/start)
     */
    @PostMapping("/{roomId}/start")
    public ResponseEntity<RoomResponse> startVoting(
            @PathVariable("roomId") String roomId,
            @RequestBody StartVotingRequest request, HttpSession session
    ) {
        requireMember(session, roomId, request.getHostId());
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
            @RequestBody SwipeMenuRequest request, HttpSession session
    ) {
        requireMember(session, roomId, request.getMemberId());
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

    /**
     * 특정 방의 현재 상태 조회 API (GET /api/rooms/{roomId})
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable("roomId") String roomId) {
        return getRoomUseCase.getRoom(roomId)
                .map(room -> ResponseEntity.ok(RoomResponse.fromDomain(room)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    public record RoomSummary(String roomId, String location, int memberCount, String status, java.time.Instant createdAt) {}

    @GetMapping
    public java.util.List<RoomSummary> listRooms() {
        return getRoomUseCase.recentRooms().stream().map(r -> new RoomSummary(r.getId(), r.getLocation(),
                r.getMembers().size(), r.getStatus().name(), r.getCreatedAt())).toList();
    }

    @GetMapping("/{roomId}/me")
    public java.util.Map<String, Object> me(@PathVariable String roomId, HttpSession session) {
        Room room = getRoomUseCase.getRoom(roomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Object actor = session.getAttribute("room:" + roomId);
        if (!(actor instanceof UUID) || room.getMembers().stream().noneMatch(m -> m.getId().equals(actor)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return java.util.Map.of("memberId", actor, "room", RoomResponse.fromDomain(room), "swiped",
                room.getSwipes().stream().filter(s -> s.getMemberId().equals(actor)).map(s -> s.getMenuName()).toList());
    }

    public record RoomAction(UUID memberId, UUID restaurantId) {}

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leave(@PathVariable String roomId, @RequestBody RoomAction request, HttpSession session) {
        requireMember(session, roomId, request.memberId());
        getRoomUseCase.leave(roomId, request.memberId());
        session.removeAttribute("room:" + roomId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roomId}/close")
    public RoomResponse close(@PathVariable String roomId, @RequestBody RoomAction request, HttpSession session) {
        requireMember(session, roomId, request.memberId());
        return RoomResponse.fromDomain(getRoomUseCase.closeVoting(roomId, request.memberId()));
    }

    @PostMapping("/{roomId}/selection")
    public RoomResponse select(@PathVariable String roomId, @RequestBody RoomAction request, HttpSession session) {
        requireMember(session, roomId, request.memberId());
        return RoomResponse.fromDomain(getRoomUseCase.selectRestaurant(roomId, request.memberId(), request.restaurantId()));
    }

    private void requireMember(HttpSession session, String roomId, UUID memberId) {
        if (memberId == null || !memberId.equals(session.getAttribute("room:" + roomId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "방에 참여한 브라우저에서 진행해 주세요.");
        }
    }
}
