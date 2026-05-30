package com.amugeonabuster.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataRoomRepository extends JpaRepository<RoomJpaEntity, String> {
}
