package com.amugeonabuster.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataRoomRepository extends JpaRepository<RoomJpaEntity, String> {

    @EntityGraph(attributePaths = {"members"})
    List<RoomJpaEntity> findTop50ByCreatedAtAfterOrderByCreatedAtDesc(Instant since);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RoomJpaEntity r where r.id = :id")
    Optional<RoomJpaEntity> findByIdWithLock(@Param("id") String id);

    List<RoomJpaEntity> findByCreatedAtBefore(Instant cutoff);
}
