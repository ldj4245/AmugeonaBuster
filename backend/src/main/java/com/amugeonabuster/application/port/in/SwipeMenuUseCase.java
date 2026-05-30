package com.amugeonabuster.application.port.in;

import com.amugeonabuster.domain.model.Room;
import lombok.Getter;

import java.util.UUID;

public interface SwipeMenuUseCase {
    Room swipeMenu(SwipeMenuCommand command);

    @Getter
    class SwipeMenuCommand {
        private final String roomId;
        private final UUID memberId;
        private final String menuName;
        private final boolean isLike;

        public SwipeMenuCommand(String roomId, UUID memberId, String menuName, boolean isLike) {
            if (roomId == null || roomId.isBlank()) {
                throw new IllegalArgumentException("방 코드는 필수입니다.");
            }
            if (memberId == null) {
                throw new IllegalArgumentException("멤버 ID는 필수입니다.");
            }
            if (menuName == null || menuName.isBlank()) {
                throw new IllegalArgumentException("메뉴명은 필수입니다.");
            }
            this.roomId = roomId;
            this.memberId = memberId;
            this.menuName = menuName;
            this.isLike = isLike;
        }
    }
}
