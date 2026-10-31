package com.amugeonabuster.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AdminUserManagementUseCase {

    record AdminUserDto(
        Long id,
        String kakaoId,
        String nickname,
        String cafeteriaName,
        String cotNo,
        String hallNo,
        String scheduledDays,
        String scheduledTime,
        boolean isEnabled,
        LocalDate lastSentDate
    ) {}

    record AdminStatsDto(
        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long sentTodayCount,
        Map<String, Long> cafeteriaDistribution
    ) {}

    List<AdminUserDto> getAllUsers();
    AdminStatsDto getStats();
    void toggleUserAlert(String kakaoId, boolean isEnabled);
    void resetUserLastSentDate(String kakaoId);
    void deleteUser(String kakaoId);
}
