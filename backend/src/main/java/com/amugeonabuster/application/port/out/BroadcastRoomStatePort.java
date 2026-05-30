package com.amugeonabuster.application.port.out;

import com.amugeonabuster.domain.model.Room;

public interface BroadcastRoomStatePort {
    void broadcastRoomState(Room room);
}
