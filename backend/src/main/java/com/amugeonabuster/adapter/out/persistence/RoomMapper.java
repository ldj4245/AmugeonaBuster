package com.amugeonabuster.adapter.out.persistence;

import com.amugeonabuster.domain.model.Member;
import com.amugeonabuster.domain.model.Room;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RoomMapper {

    /**
     * JPA Entity -> Pure Domain Model 변환
     */
    public Room toDomainModel(RoomJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        List<Member> domainMembers = jpaEntity.getMembers().stream()
                .map(memberEntity -> Member.builder()
                        .id(memberEntity.getId())
                        .nickname(memberEntity.getNickname())
                        .isReady(memberEntity.isReady())
                        .build())
                .collect(Collectors.toList());

        return Room.builder()
                .id(jpaEntity.getId())
                .hostId(jpaEntity.getHostId())
                .location(jpaEntity.getLocation())
                .status(jpaEntity.getStatus())
                .members(domainMembers)
                .build();
    }

    /**
     * Pure Domain Model -> JPA Entity 변환 (Cascade를 위한 연관관계 설정 포함)
     */
    public RoomJpaEntity toJpaEntity(Room domainModel) {
        if (domainModel == null) {
            return null;
        }

        RoomJpaEntity jpaEntity = RoomJpaEntity.builder()
                .id(domainModel.getId())
                .hostId(domainModel.getHostId())
                .location(domainModel.getLocation())
                .status(domainModel.getStatus())
                .build();

        // 도메인 내부에 격리된 멤버들을 양방향 JPA Entity 관계로 바인딩
        domainModel.getMembers().forEach(domainMember -> {
            MemberJpaEntity memberJpaEntity = MemberJpaEntity.builder()
                    .id(domainMember.getId())
                    .nickname(domainMember.getNickname())
                    .isReady(domainMember.isReady())
                    .build();
            jpaEntity.addMember(memberJpaEntity);
        });

        return jpaEntity;
    }
}
