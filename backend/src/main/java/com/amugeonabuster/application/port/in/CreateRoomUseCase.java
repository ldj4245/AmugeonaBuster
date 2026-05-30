package com.amugeonabuster.application.port.in;

import com.amugeonabuster.domain.model.Room;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
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
        private final int maxSwipeCount;
        private final List<String> customMenus;

        public CreateRoomCommand(String hostNickname, String location, int maxSwipeCount) {
            this.hostNickname = Objects.requireNonNull(hostNickname, "방장 닉네임은 필수입니다.");
            if (hostNickname.trim().isEmpty()) {
                throw new IllegalArgumentException("방장 닉네임은 공백일 수 없습니다.");
            }
            this.location = Objects.requireNonNull(location, "약속 장소는 필수입니다.");
            if (location.trim().isEmpty()) {
                throw new IllegalArgumentException("약속 장소는 공백일 수 없습니다.");
            }
            this.maxSwipeCount = maxSwipeCount != 0 ? maxSwipeCount : 15;
            this.customMenus = com.amugeonabuster.domain.model.DefaultMenus.MENUS.subList(0, this.maxSwipeCount);
        }

        public CreateRoomCommand(String hostNickname, String location, List<String> customMenus) {
            this.hostNickname = Objects.requireNonNull(hostNickname, "방장 닉네임은 필수입니다.");
            if (hostNickname.trim().isEmpty()) {
                throw new IllegalArgumentException("방장 닉네임은 공백일 수 없습니다.");
            }
            this.location = Objects.requireNonNull(location, "약속 장소는 필수입니다.");
            if (location.trim().isEmpty()) {
                throw new IllegalArgumentException("약속 장소는 공백일 수 없습니다.");
            }
            this.customMenus = customMenus != null && !customMenus.isEmpty() ? List.copyOf(customMenus) : com.amugeonabuster.domain.model.DefaultMenus.MENUS;
            this.maxSwipeCount = this.customMenus.size();
        }
    }
}
