package com.amugeonabuster.application.port.in;

import com.amugeonabuster.domain.model.Room;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

public interface CreateRoomUseCase {
    Room createRoom(CreateRoomCommand command);

    /**
     * 자가 검증 불변 입력 커맨드 (Self-Validating Command)
     */
    @Getter
    @ToString
    @EqualsAndHashCode
    class CreateRoomCommand {
        private final String hostNickname;
        private final String location;

        public CreateRoomCommand(String hostNickname, String location) {
            this.hostNickname = Objects.requireNonNull(hostNickname, "방장 닉네임은 필수입니다.");
            if (hostNickname.trim().isEmpty()) {
                throw new IllegalArgumentException("방장 닉네임은 공백일 수 없습니다.");
            }
            this.location = Objects.requireNonNull(location, "약속 장소는 필수입니다.");
            if (location.trim().isEmpty()) {
                throw new IllegalArgumentException("약속 장소는 공백일 수 없습니다.");
            }
        }
    }
}
