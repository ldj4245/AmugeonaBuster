package com.amugeonabuster.application.service;

import com.amugeonabuster.application.port.in.AdminUserManagementUseCase;
import com.amugeonabuster.application.port.out.WelstoryAlertPort;
import com.amugeonabuster.domain.model.WelstoryAlertSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminUserManagementService implements AdminUserManagementUseCase {

    private final WelstoryAlertPort welstoryAlertPort;

    @Override
    public List<AdminUserDto> getAllUsers() {
        return welstoryAlertPort.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public AdminStatsDto getStats() {
        List<WelstoryAlertSettings> all = welstoryAlertPort.findAll();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        long active = all.stream().filter(WelstoryAlertSettings::isEnabled).count();
        long sentToday = all.stream().filter(s -> today.equals(s.getLastSentDate())).count();
        Map<String, Long> distribution = all.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCafeteriaName() != null ? s.getCafeteriaName() : "기타",
                        Collectors.counting()
                ));
        return new AdminStatsDto(all.size(), active, all.size() - active, sentToday, distribution);
    }

    @Override
    @Transactional
    public void toggleUserAlert(String kakaoId, boolean isEnabled) {
        welstoryAlertPort.findByKakaoId(kakaoId).ifPresent(s -> {
            s.setEnabled(isEnabled);
            welstoryAlertPort.save(s);
            log.info("Admin updated alert status for kakaoId {} to {}", kakaoId, isEnabled);
        });
    }

    @Override
    @Transactional
    public void resetUserLastSentDate(String kakaoId) {
        welstoryAlertPort.findByKakaoId(kakaoId).ifPresent(s -> {
            s.setLastSentDate(null);
            welstoryAlertPort.save(s);
            log.info("Admin reset lastSentDate for kakaoId {}", kakaoId);
        });
    }

    @Override
    @Transactional
    public void deleteUser(String kakaoId) {
        welstoryAlertPort.deleteByKakaoId(kakaoId);
        log.info("Admin deleted user subscription for kakaoId {}", kakaoId);
    }

    private AdminUserDto toDto(WelstoryAlertSettings s) {
        return new AdminUserDto(
                s.getId(),
                s.getKakaoId(),
                s.getNickname(),
                s.getCafeteriaName(),
                s.getCotNo(),
                s.getHallNo(),
                s.getScheduledDays(),
                s.getScheduledTime(),
                s.isEnabled(),
                s.getLastSentDate()
        );
    }
}
