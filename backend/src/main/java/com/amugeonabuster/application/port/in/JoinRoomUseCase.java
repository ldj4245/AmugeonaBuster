package com.amugeonabuster.application.port.in;

import com.amugeonabuster.domain.model.Room;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

public interface JoinRoomUseCase {
    Room joinRoom(JoinRoomCommand command);

    /**
     * 자가 검증 불변 입장 커맨드 (Self-Validating Command)
     */
    @Getter
    @ToString
    @EqualsAndHashCode
    class JoinRoomCommand {
        private final String roomId;
        private final String guestNickname;

        public JoinRoomCommand(String roomId, String guestNickname) {
            this.roomId = Objects.requireNonNull(roomId, "방 코드는 필수입니다.");
            if (roomId.trim().isEmpty()) {
                throw new IllegalArgumentException("방 코드는 공백일 수 없습니다.");
            }
            this.guestNickname = Objects.requireNonNull(guestNickname, "참가자 닉네임은 필수입니다.");
            if (guestNickname.trim().isEmpty()) {
                throw new IllegalArgumentException("참가자 닉네임은 공백일 수 없습니다.");
            }
        }
    }
}
