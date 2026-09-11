package com.amugeonabuster.application.port.out;

import com.amugeonabuster.domain.model.Room;

import java.util.Optional;

public interface LoadRoomPort {
    Optional<Room> loadRoom(String roomId);
    default java.util.List<Room> recentRooms() { return java.util.List.of(); }
}
