package com.amugeonabuster.adapter.out.persistence;

import com.amugeonabuster.domain.model.Member;
import com.amugeonabuster.domain.model.Restaurant;
import com.amugeonabuster.domain.model.Room;
import com.amugeonabuster.domain.model.RoomStatus;
import com.amugeonabuster.domain.model.Swipe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
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

    @Test
    @DisplayName("스와이프 데이터 및 최종 선정 맛집 정보까지 RDB 영속성 연동 및 복원에 성공한다")
    void saveAndLoadRoom_withSwipesAndRestaurants_success() {
        // given
        UUID hostId = UUID.randomUUID();
        Member host = Member.builder().id(hostId).nickname("방장").isReady(true).build();
        
        Swipe swipe1 = Swipe.builder().memberId(hostId).menuName("삼겹살").isLike(true).build();
        Swipe swipe2 = Swipe.builder().memberId(hostId).menuName("초밥").isLike(false).build();

        List<Restaurant> restaurants = List.of(
                Restaurant.builder().name("맛있는 삼겹살집").address("역삼동").latitude(37.49).longitude(127.03).phone("02-000-0000").build(),
                Restaurant.builder().name("인생 삼겹살").address("서초동").latitude(37.48).longitude(127.02).phone("02-111-1111").build()
        );

        Room room = Room.builder()
                .id("ROOM-MATCHED")
                .hostId(hostId)
                .location("강남역")
                .status(RoomStatus.COMPLETED)
                .winningMenu("삼겹살")
                .members(List.of(host))
                .swipes(List.of(swipe1, swipe2))
                .matchedRestaurants(restaurants)
                .build();

        // when
        adapter.saveRoom(room);

        // then
        Optional<Room> loadedRoom = adapter.loadRoom("ROOM-MATCHED");
        assertThat(loadedRoom).isPresent();
        assertThat(loadedRoom.get().getWinningMenu()).isEqualTo("삼겹살");
        assertThat(loadedRoom.get().getStatus()).isEqualTo(RoomStatus.COMPLETED);
        
        // 스와이프 복원 검증
        assertThat(loadedRoom.get().getSwipes()).hasSize(2);
        assertThat(loadedRoom.get().getSwipes().get(0).getMenuName()).isEqualTo("삼겹살");
        assertThat(loadedRoom.get().getSwipes().get(0).isLike()).isTrue();
        assertThat(loadedRoom.get().getSwipes().get(1).getMenuName()).isEqualTo("초밥");
        assertThat(loadedRoom.get().getSwipes().get(1).isLike()).isFalse();

        // 추천 맛집 복원 검증
        assertThat(loadedRoom.get().getMatchedRestaurants()).hasSize(2);
        assertThat(loadedRoom.get().getMatchedRestaurants().get(0).getName()).isEqualTo("맛있는 삼겹살집");
        assertThat(loadedRoom.get().getMatchedRestaurants().get(1).getName()).isEqualTo("인생 삼겹살");
    }
}
