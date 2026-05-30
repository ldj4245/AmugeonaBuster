package com.amugeonabuster.adapter.out.persistence;

import com.amugeonabuster.application.port.out.LoadRoomPort;
import com.amugeonabuster.application.port.out.SaveRoomPort;
import com.amugeonabuster.domain.model.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoomPersistenceAdapter implements SaveRoomPort, LoadRoomPort {

    private final SpringDataRoomRepository repository;
    private final RoomMapper mapper;

    @Override
    public void saveRoom(Room room) {
        RoomJpaEntity jpaEntity = mapper.toJpaEntity(room);
        repository.save(jpaEntity);
    }

    @Override
    public Optional<Room> loadRoom(String roomId) {
        return repository.findById(roomId)
                .map(mapper::toDomainModel);
    }
}
