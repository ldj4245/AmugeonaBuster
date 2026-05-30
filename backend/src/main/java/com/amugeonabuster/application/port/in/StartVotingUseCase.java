package com.amugeonabuster.application.port.in;

import com.amugeonabuster.domain.model.Room;
import lombok.Getter;

import java.util.UUID;

public interface StartVotingUseCase {
    Room startVoting(StartVotingCommand command);

    @Getter
    class StartVotingCommand {
        private final String roomId;
        private final UUID hostId;

        public StartVotingCommand(String roomId, UUID hostId) {
            if (roomId == null || roomId.isBlank()) {
                throw new IllegalArgumentException("방 코드는 필수입니다.");
            }
            if (hostId == null) {
                throw new IllegalArgumentException("방장 ID는 필수입니다.");
            }
            this.roomId = roomId;
            this.hostId = hostId;
        }
    }
}
