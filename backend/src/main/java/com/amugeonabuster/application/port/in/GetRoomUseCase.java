package com.amugeonabuster.application.port.in;

import com.amugeonabuster.domain.model.Room;
import java.util.Optional;

public interface GetRoomUseCase {
    Optional<Room> getRoom(String roomId);
}
