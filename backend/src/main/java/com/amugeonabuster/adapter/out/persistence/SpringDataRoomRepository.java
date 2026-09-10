package com.amugeonabuster.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface SpringDataRoomRepository extends JpaRepository<RoomJpaEntity, String> {
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RoomJpaEntity> findById(String id);
}
