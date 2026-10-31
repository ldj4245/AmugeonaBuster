package com.amugeonabuster.application.port.out;

import com.amugeonabuster.domain.model.Room;

import java.util.List;
import java.util.Optional;

public interface LoadRoomPort {
    Optional<Room> loadRoom(String roomId);
    default Optional<Room> loadRoomWithLock(String roomId) { return loadRoom(roomId); }
    default List<Room> recentRooms() { return List.of(); }
}
