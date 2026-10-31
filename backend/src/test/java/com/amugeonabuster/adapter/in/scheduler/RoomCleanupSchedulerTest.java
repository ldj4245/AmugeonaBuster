package com.amugeonabuster.adapter.in.scheduler;

import com.amugeonabuster.application.port.in.CleanupExpiredRoomsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomCleanupSchedulerTest {

    @Mock
    private CleanupExpiredRoomsUseCase cleanupExpiredRoomsUseCase;

    @InjectMocks
    private RoomCleanupScheduler scheduler;

    @Test
    @DisplayName("스케줄러 동작 시 만료 방 정리 유스케이스가 정상 호출된다")
    void cleanupExpiredRoomsSuccess() {
        when(cleanupExpiredRoomsUseCase.cleanupExpiredRooms()).thenReturn(3);

        scheduler.cleanupExpiredRooms();

        verify(cleanupExpiredRoomsUseCase, times(1)).cleanupExpiredRooms();
    }

    @Test
    @DisplayName("정리 도중 예외가 발생해도 스케줄러가 비정상 종료되지 않고 예외를 처리한다")
    void cleanupExpiredRoomsHandlesException() {
        when(cleanupExpiredRoomsUseCase.cleanupExpiredRooms()).thenThrow(new RuntimeException("DB error"));

        scheduler.cleanupExpiredRooms();

        verify(cleanupExpiredRoomsUseCase, times(1)).cleanupExpiredRooms();
    }
}
