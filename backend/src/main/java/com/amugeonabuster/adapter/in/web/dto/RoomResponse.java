package com.amugeonabuster.adapter.in.web.dto;

import com.amugeonabuster.domain.model.Room;
import com.amugeonabuster.domain.model.RoomStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
public class RoomResponse {
    private final String roomId;
    private final UUID hostId;
    private final String location;
    private final RoomStatus status;
    private final List<MemberResponse> members;

    /**
     * 도메인 객체로부터 API 응답 포맷으로 고속 변환
     */
    public static RoomResponse fromDomain(Room room) {
        if (room == null) {
            return null;
        }

        List<MemberResponse> memberResponses = room.getMembers().stream()
                .map(m -> MemberResponse.builder()
                        .id(m.getId())
                        .nickname(m.getNickname())
                        .isReady(m.isReady())
                        .build())
                .collect(Collectors.toList());

        return RoomResponse.builder()
                .roomId(room.getId())
                .hostId(room.getHostId())
                .location(room.getLocation())
                .status(room.getStatus())
                .members(memberResponses)
                .build();
    }
}
