package com.amugeonabuster.application.port.in;

import com.amugeonabuster.domain.model.Room;
import java.util.Optional;

public interface GetRoomUseCase {
    Optional<Room> getRoom(String roomId);
    default java.util.List<Room> recentRooms() { return java.util.List.of(); }
    default Room leave(String roomId, java.util.UUID actor) { throw new UnsupportedOperationException(); }
    default Room closeVoting(String roomId, java.util.UUID actor) { throw new UnsupportedOperationException(); }
    default Room selectRestaurant(String roomId, java.util.UUID actor, java.util.UUID restaurantId) { throw new UnsupportedOperationException(); }
}
