package com.amugeonabuster.adapter.out.persistence;

import com.amugeonabuster.application.port.out.WelstoryAlertPort;
import com.amugeonabuster.domain.model.WelstoryAlertSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WelstoryPersistenceAdapter implements WelstoryAlertPort {

    private final SpringDataWelstoryRepository repository;

    @Override
    public Optional<WelstoryAlertSettings> findByKakaoId(String kakaoId) {
        return repository.findByKakaoId(kakaoId).map(this::toDomain);
    }

    @Override
    public List<WelstoryAlertSettings> findAllByScheduledTimeAndIsEnabled(String scheduledTime) {
        return repository.findAllByScheduledTimeAndIsEnabledTrue(scheduledTime).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public WelstoryAlertSettings save(WelstoryAlertSettings setting) {
        WelstoryAlertSettingsJpaEntity entity = toEntity(setting);
        WelstoryAlertSettingsJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private WelstoryAlertSettings toDomain(WelstoryAlertSettingsJpaEntity entity) {
        if (entity == null) return null;
        return WelstoryAlertSettings.builder()
                .id(entity.getId())
                .kakaoId(entity.getKakaoId())
                .nickname(entity.getNickname())
                .kakaoAccessToken(entity.getKakaoAccessToken())
                .kakaoRefreshToken(entity.getKakaoRefreshToken())
                .cotNo(entity.getCotNo())
                .hallNo(entity.getHallNo())
                .cafeteriaName(entity.getCafeteriaName())
                .scheduledDays(entity.getScheduledDays())
                .scheduledTime(entity.getScheduledTime())
                .isEnabled(entity.isEnabled())
                .lastSentDate(entity.getLastSentDate())
                .build();
    }

    private WelstoryAlertSettingsJpaEntity toEntity(WelstoryAlertSettings domain) {
        if (domain == null) return null;
        return WelstoryAlertSettingsJpaEntity.builder()
                .id(domain.getId())
                .kakaoId(domain.getKakaoId())
                .nickname(domain.getNickname())
                .kakaoAccessToken(domain.getKakaoAccessToken())
                .kakaoRefreshToken(domain.getKakaoRefreshToken())
                .cotNo(domain.getCotNo())
                .hallNo(domain.getHallNo())
                .cafeteriaName(domain.getCafeteriaName())
                .scheduledDays(domain.getScheduledDays())
                .scheduledTime(domain.getScheduledTime())
                .isEnabled(domain.isEnabled())
                .lastSentDate(domain.getLastSentDate())
                .build();
    }
}
