package com.amugeonabuster.application.service;

import com.amugeonabuster.application.port.in.CleanupExpiredRoomsUseCase;
import com.amugeonabuster.application.port.in.CreateRoomUseCase;
import com.amugeonabuster.application.port.in.GetRoomUseCase;
import com.amugeonabuster.application.port.in.JoinRoomUseCase;
import com.amugeonabuster.application.port.in.StartVotingUseCase;
import com.amugeonabuster.application.port.in.SwipeMenuUseCase;
import com.amugeonabuster.application.port.out.BroadcastRoomStatePort;
import com.amugeonabuster.application.port.out.LoadRoomPort;
import com.amugeonabuster.application.port.out.RecommendRestaurantsPort;
import com.amugeonabuster.application.port.out.SaveRoomPort;
import com.amugeonabuster.domain.model.Member;
import com.amugeonabuster.domain.model.Restaurant;
import com.amugeonabuster.domain.model.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService implements CreateRoomUseCase, GetRoomUseCase, JoinRoomUseCase, StartVotingUseCase, SwipeMenuUseCase, CleanupExpiredRoomsUseCase {

    private final SaveRoomPort saveRoomPort;
    private final LoadRoomPort loadRoomPort;
    private final BroadcastRoomStatePort broadcastRoomStatePort;
    private final RecommendRestaurantsPort recommendRestaurantsPort;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public Room createRoom(CreateRoomCommand command) {
        String roomId = generateUniqueRoomId();
        UUID hostId = UUID.randomUUID();

        // 방장 생성 (방장은 자동 준비완료 상태)
        Member host = Member.builder()
                .id(hostId)
                .nickname(command.getHostNickname())
                .isReady(true)
                .build();

        // 방 생성
        Room room = Room.builder()
                .id(roomId)
                .hostId(hostId)
                .createdAt(Instant.now())
                .location(command.getLocation())
                .locationAddress(command.getLocationAddress())
                .locationPlaceId(command.getLocationPlaceId())
                .latitude(command.getLatitude())
                .longitude(command.getLongitude())
                .customMenus(command.getCustomMenus())
                .build();

        room.joinMember(host);
        saveRoomPort.saveRoom(room);

        return room;
    }

    @Override
    @Transactional
    public Room joinRoom(JoinRoomCommand command) {
        // 방 정보 획득
        Room room = loadRoomPort.loadRoom(command.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다. 코드: " + command.getRoomId()));

        // 신규 유저 생성 (대기방 입장이므로 기본 준비 상태는 false)
        Member guest = Member.builder()
                .nickname(command.getGuestNickname())
                .isReady(false)
                .build();

        // 도메인 내부 가입 규칙 가동
        room.joinMember(guest);
        saveRoomPort.saveRoom(room);

        // 실시간 대기실 유저 목록 자동 브로드캐스트 작동
        broadcastRoomStatePort.broadcastRoomState(room);

        return room;
    }

    @Override
    @Transactional
    public Room startVoting(StartVotingCommand command) {
        Room room = loadRoomPort.loadRoom(command.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다. 코드: " + command.getRoomId()));

        room.startVoting(command.getHostId());
        saveRoomPort.saveRoom(room);

        // 실시간 소켓 브로드캐스트 (대기방 화면에서 게임 시작 스와이프 화면으로 전환 유도)
        broadcastRoomStatePort.broadcastRoomState(room);

        return room;
    }

    /**
     * 외부 카카오 API 호출(recommendRestaurantsPort.recommend)을 DB 트랜잭션 밖에서 실행하여
     * HTTP 지연 중 DB 커넥션 및 락 점유를 원천 방지한다.
     */
    @Override
    public Room swipeMenu(SwipeMenuCommand command) {
        Room room = loadRoomPort.loadRoom(command.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다. 코드: " + command.getRoomId()));

        // 도메인의 스와이프 투표 규칙 가동
        room.swipeMenu(command.getMemberId(), command.getMenuName(), command.isLike());

        // 전원 스와이프 완료 여부 확인 및 1위 선정 가동
        if (room.isAllMembersCompletedSwiping()) {
            room.determineWinningMenu();

            List<Restaurant> recommended = room.hasLocationCoordinates()
                    ? recommendRestaurantsPort.recommend(room.getWinningMenu(), room.getLocation(),
                            room.getLatitude(), room.getLongitude())
                    : recommendRestaurantsPort.recommend(room.getWinningMenu(), room.getLocation());
            room.associateMatchedRestaurants(recommended);
        }

        // 실시간 소켓 브로드캐스트를 saveRoom 이전에 실행 → swipes가 in-memory에 온전히 살아있을 때 voteStats 계산 보장
        broadcastRoomStatePort.broadcastRoomState(room);

        saveRoomPort.saveRoom(room);

        return room;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> recentRooms() { return loadRoomPort.recentRooms(); }

    private Room requireRoom(String id) {
        Room room = loadRoomPort.loadRoom(id).orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));
        if (room.isExpired()) throw new IllegalArgumentException("만료된 방입니다.");
        return room;
    }

    private Room publish(Room room) {
        broadcastRoomStatePort.broadcastRoomState(room);
        saveRoomPort.saveRoom(room);
        return room;
    }

    @Override
    @Transactional
    public Room leave(String id, UUID actor) {
        Room room = loadRoomPort.loadRoom(id).orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));
        room.leave(actor);
        if (!room.isExpired() && room.getStatus() == com.amugeonabuster.domain.model.RoomStatus.PLAYING && room.isAllMembersCompletedSwiping()) {
            finishVoting(room);
        }
        return publish(room);
    }

    private void finishVoting(Room room) {
        room.determineWinningMenu();
        room.associateMatchedRestaurants(room.hasLocationCoordinates()
            ? recommendRestaurantsPort.recommend(room.getWinningMenu(), room.getLocation(), room.getLatitude(), room.getLongitude())
            : recommendRestaurantsPort.recommend(room.getWinningMenu(), room.getLocation()));
    }

    /**
     * 외부 카카오 API 호출(recommendRestaurantsPort.recommend)을 DB 트랜잭션 밖에서 실행한다.
     */
    @Override
    public Room closeVoting(String id, UUID actor) {
        Room room = requireRoom(id);
        if (!room.getHostId().equals(actor)) throw new IllegalArgumentException("방장만 투표를 마감할 수 있습니다.");
        if (room.getSwipes().isEmpty()) throw new IllegalArgumentException("한 표 이상 모인 뒤 마감해 주세요.");
        finishVoting(room);
        return publish(room);
    }

    @Override
    @Transactional
    public Room selectRestaurant(String id, UUID actor, UUID restaurantId) {
        Room room = requireRoom(id);
        room.selectRestaurant(actor, restaurantId);
        return publish(room);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Room> getRoom(String roomId) {
        return loadRoomPort.loadRoom(roomId);
    }

    @Override
    @Transactional
    public int cleanupExpiredRooms() {
        // 24시간 이상 지난 만료 방 정리
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        return saveRoomPort.deleteRoomsCreatedBefore(cutoff);
    }

    /**
     * 보안 6자리 대문자/숫자 조합 방 코드 생성기
     */
    private String generateUniqueRoomId() {
        StringBuilder sb = new StringBuilder("ROOM-");
        for (int i = 0; i < 6; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
