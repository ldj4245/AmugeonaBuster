package com.amugeonabuster.adapter.in.web;

import com.amugeonabuster.application.port.in.AdminUserManagementUseCase;
import com.amugeonabuster.application.port.in.AdminUserManagementUseCase.AdminStatsDto;
import com.amugeonabuster.application.port.in.AdminUserManagementUseCase.AdminUserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    private AdminUserManagementUseCase useCase;
    private AdminController controller;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        useCase = mock(AdminUserManagementUseCase.class);
        controller = new AdminController(useCase);
        ReflectionTestUtils.setField(controller, "adminSecretKey", "test-secret-key");
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void rejectsLoginWithWrongPassword() {
        MockHttpSession session = new MockHttpSession();
        assertThatThrownBy(() -> controller.login(new AdminController.LoginRequest("wrong"), session))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(session.getAttribute("isAdminAuthenticated")).isNull();
    }

    @Test
    void acceptsLoginWithCorrectPasswordAndSetsSession() {
        MockHttpSession session = new MockHttpSession();
        var response = controller.login(new AdminController.LoginRequest("test-secret-key"), session);

        assertThat(response.getBody()).containsEntry("authenticated", true);
        assertThat(session.getAttribute("isAdminAuthenticated")).isEqualTo(true);
    }

    @Test
    void rejectsUnauthenticatedUsersAccess() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThatThrownBy(() -> controller.getUsers(request))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(useCase);
    }

    @Test
    void allowsAccessWithHeaderKeyAndReturnsUserListWithoutTokens() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Admin-Key", "test-secret-key");

        AdminUserDto user = new AdminUserDto(
                1L, "123456", "홍길동", "삼성 DSR 타워 웰스토리", "WEL_DSR", "HALL_01",
                "1,2,3,4,5", "11:30", true, LocalDate.of(2026, 10, 31)
        );
        when(useCase.getAllUsers()).thenReturn(List.of(user));

        var response = controller.getUsers(request);
        String json = objectMapper.writeValueAsString(response.getBody());

        assertThat(response.getBody()).hasSize(1);
        assertThat(json).contains("홍길동").contains("WEL_DSR");
        assertThat(json).doesNotContain("kakaoAccessToken", "kakaoRefreshToken");
    }

    @Test
    void toggleResetAndDeleteDelegateToUseCase() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("isAdminAuthenticated", true);

        controller.toggleAlert("123456", new AdminController.ToggleRequest(false), request);
        verify(useCase).toggleUserAlert("123456", false);

        controller.resetSend("123456", request);
        verify(useCase).resetUserLastSentDate("123456");

        controller.deleteUser("123456", request);
        verify(useCase).deleteUser("123456");
    }

    @Test
    void returnsAdminStats() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("isAdminAuthenticated", true);

        when(useCase.getStats()).thenReturn(new AdminStatsDto(10, 8, 2, 5, Map.of("삼성 DSR", 6L, "수원 R5", 4L)));

        var stats = controller.getStats(request).getBody();
        assertThat(stats.totalUsers()).isEqualTo(10);
        assertThat(stats.activeUsers()).isEqualTo(8);
        assertThat(stats.inactiveUsers()).isEqualTo(2);
        assertThat(stats.sentTodayCount()).isEqualTo(5);
    }
}
