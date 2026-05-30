package com.amugeonabuster.adapter.out.persistence;

import com.amugeonabuster.domain.model.Member;
import com.amugeonabuster.domain.model.Room;
import com.amugeonabuster.domain.model.RoomStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({RoomPersistenceAdapter.class, RoomMapper.class})
class RoomPersistenceAdapterTest {

    @Autowired
    private RoomPersistenceAdapter adapter;

    @Autowired
    private SpringDataRoomRepository repository;

    @Test
    @DisplayName("방 도메인 모델을 넘겼을 때 데이터베이스에 일괄 Cascade 저장에 성공한다")
    void saveRoom_success() {
        // given
        UUID hostId = UUID.randomUUID();
        Room room = Room.builder()
                .id("ROOM-ABC123")
                .hostId(hostId)
                .location("강남역")
                .status(RoomStatus.LOBBY)
                .build();

        Member host = Member.builder()
                .id(hostId)
                .nickname("김방장")
                .isReady(true)
                .build();
        room.joinMember(host);

        // when
        adapter.saveRoom(room);

        // then
        Optional<RoomJpaEntity> savedEntity = repository.findById("ROOM-ABC123");
        assertThat(savedEntity).isPresent();
        assertThat(savedEntity.get().getLocation()).isEqualTo("강남역");
        assertThat(savedEntity.get().getMembers()).hasSize(1);
        assertThat(savedEntity.get().getMembers().get(0).getNickname()).isEqualTo("김방장");
    }

    @Test
    @DisplayName("저장된 방 코드로 로드 포트를 실행하면 도메인 모델로 완벽히 복원된다")
    void loadRoom_success() {
        // given
        RoomJpaEntity roomJpaEntity = RoomJpaEntity.builder()
                .id("ROOM-XYZ999")
                .hostId(UUID.randomUUID())
                .location("홍대입구")
                .status(RoomStatus.LOBBY)
                .build();

        MemberJpaEntity memberJpaEntity = MemberJpaEntity.builder()
                .id(UUID.randomUUID())
                .nickname("이참가")
                .isReady(false)
                .build();
        roomJpaEntity.addMember(memberJpaEntity);

        repository.save(roomJpaEntity);

        // when
        Optional<Room> loadedRoom = adapter.loadRoom("ROOM-XYZ999");

        // then
        assertThat(loadedRoom).isPresent();
        assertThat(loadedRoom.get().getLocation()).isEqualTo("홍대입구");
        assertThat(loadedRoom.get().getMembers()).hasSize(1);
        assertThat(loadedRoom.get().getMembers().get(0).getNickname()).isEqualTo("이참가");
    }
}
