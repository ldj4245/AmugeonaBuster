package com.amugeonabuster.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataWelstoryRepository extends JpaRepository<WelstoryAlertSettingsJpaEntity, Long> {
    
    Optional<WelstoryAlertSettingsJpaEntity> findByKakaoId(String kakaoId);
    
    List<WelstoryAlertSettingsJpaEntity> findAllByScheduledTimeAndIsEnabledTrue(String scheduledTime);
}
