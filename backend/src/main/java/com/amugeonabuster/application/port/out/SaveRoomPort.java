package com.amugeonabuster.application.port.out;

import com.amugeonabuster.domain.model.Room;

import java.time.Instant;

public interface SaveRoomPort {
    void saveRoom(Room room);
    default int deleteRoomsCreatedBefore(Instant cutoff) { return 0; }
}
