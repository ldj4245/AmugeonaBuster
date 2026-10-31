package com.amugeonabuster.adapter.out.persistence;

import com.amugeonabuster.application.port.out.LoadRoomPort;
import com.amugeonabuster.application.port.out.SaveRoomPort;
import com.amugeonabuster.domain.model.Member;
import com.amugeonabuster.domain.model.Restaurant;
import com.amugeonabuster.domain.model.Room;
import com.amugeonabuster.domain.model.Swipe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoomPersistenceAdapter implements SaveRoomPort, LoadRoomPort {

    private final SpringDataRoomRepository repository;
    private final RoomMapper mapper;

    @Override
    public List<Room> recentRooms() {
        return repository.findTop50ByCreatedAtAfterOrderByCreatedAtDesc(Instant.now().minusSeconds(10800))
                .stream().map(mapper::toSummaryDomainModel).filter(r -> !r.getMembers().isEmpty()).toList();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void saveRoom(Room room) {
        Optional<RoomJpaEntity> existingOpt = repository.findById(room.getId());
        if (existingOpt.isEmpty()) {
            repository.save(mapper.toJpaEntity(room));
            return;
        }

        RoomJpaEntity existing = existingOpt.get();
        existing.updateState(room.getHostId(), room.getStatus(), room.getWinningMenu(), room.getSelectedRestaurantId());

        // 증분 멤버 갱신 (퇴장 멤버 제거 및 신규 멤버 추가)
        Set<UUID> domainMemberIds = room.getMembers().stream().map(Member::getId).collect(Collectors.toSet());
        existing.getMembers().removeIf(m -> !domainMemberIds.contains(m.getId()));
        Set<UUID> existingMemberIds = existing.getMembers().stream().map(MemberJpaEntity::getId).collect(Collectors.toSet());
        for (Member m : room.getMembers()) {
            if (!existingMemberIds.contains(m.getId())) {
                existing.addMember(MemberJpaEntity.builder()
                        .id(m.getId())
                        .nickname(m.getNickname())
                        .isReady(m.isReady())
                        .build());
            }
        }

        // 증분 스와이프 저장 (O(N^2) 전체 DELETE 후 재INSERT 방지)
        Set<UUID> remainingMemberIds = room.getMembers().stream().map(Member::getId).collect(Collectors.toSet());
        existing.getSwipes().removeIf(s -> !remainingMemberIds.contains(s.getMemberId()));
        Set<String> existingSwipeKeys = existing.getSwipes().stream()
                .map(s -> s.getMemberId() + ":" + s.getMenuName())
                .collect(Collectors.toSet());
        for (Swipe s : room.getSwipes()) {
            String key = s.getMemberId() + ":" + s.getMenuName();
            if (!existingSwipeKeys.contains(key)) {
                existing.addSwipe(SwipeJpaEntity.builder()
                        .memberId(s.getMemberId())
                        .menuName(s.getMenuName())
                        .isLike(s.isLike())
                        .build());
            }
        }

        // 매칭된 맛집 추가
        Set<UUID> existingRestIds = existing.getMatchedRestaurants().stream()
                .map(RestaurantJpaEntity::getId).collect(Collectors.toSet());
        for (Restaurant r : room.getMatchedRestaurants()) {
            if (!existingRestIds.contains(r.getId())) {
                existing.addRestaurant(mapper.toRestaurantEntity(r));
            }
        }

        repository.save(existing);
    }

    @Override
    public Optional<Room> loadRoom(String roomId) {
        return repository.findById(roomId)
                .map(mapper::toDomainModel);
    }

    @Override
    public Optional<Room> loadRoomWithLock(String roomId) {
        return repository.findByIdWithLock(roomId)
                .map(mapper::toDomainModel);
    }

    @Override
    public int deleteRoomsCreatedBefore(Instant cutoff) {
        List<RoomJpaEntity> expired = repository.findByCreatedAtBefore(cutoff);
        if (expired.isEmpty()) return 0;
        int count = expired.size();
        repository.deleteAll(expired);
        return count;
    }
}
