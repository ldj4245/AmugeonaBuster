package com.amugeonabuster.application.service;

import com.amugeonabuster.application.port.in.CreateRoomUseCase;
import com.amugeonabuster.application.port.in.JoinRoomUseCase;
import com.amugeonabuster.application.port.out.LoadRoomPort;
import com.amugeonabuster.application.port.out.SaveRoomPort;
import com.amugeonabuster.domain.model.Member;
import com.amugeonabuster.domain.model.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomService implements CreateRoomUseCase, JoinRoomUseCase {

    private final SaveRoomPort saveRoomPort;
    private final LoadRoomPort loadRoomPort;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
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
                .location(command.getLocation())
                .build();

        room.joinMember(host);
        saveRoomPort.saveRoom(room);

        return room;
    }

    @Override
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

        return room;
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
