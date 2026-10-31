package com.amugeonabuster.adapter.in.web;

import com.amugeonabuster.application.port.in.AdminUserManagementUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final String ADMIN_AUTH_SESSION_KEY = "isAdminAuthenticated";
    private final AdminUserManagementUseCase adminUseCase;

    @Value("${admin.secret-key:amugeona-admin-2026}")
    private String adminSecretKey;

    public record LoginRequest(String secretKey) {}
    public record ToggleRequest(boolean isEnabled) {}

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request, HttpSession session) {
        if (request == null || request.secretKey() == null || !request.secretKey().trim().equals(adminSecretKey.trim())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다.");
        }
        session.setAttribute(ADMIN_AUTH_SESSION_KEY, true);
        session.setMaxInactiveInterval(60 * 60 * 2); // 2 hours
        return ResponseEntity.ok(Map.of("authenticated", true));
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> check(HttpServletRequest request) {
        boolean auth = isAuthorized(request);
        return ResponseEntity.ok(Map.of("authenticated", auth));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.removeAttribute(ADMIN_AUTH_SESSION_KEY);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserManagementUseCase.AdminUserDto>> getUsers(HttpServletRequest request) {
        validateAdmin(request);
        return ResponseEntity.ok(adminUseCase.getAllUsers());
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminUserManagementUseCase.AdminStatsDto> getStats(HttpServletRequest request) {
        validateAdmin(request);
        return ResponseEntity.ok(adminUseCase.getStats());
    }

    @PostMapping("/users/{kakaoId}/toggle")
    public ResponseEntity<Void> toggleAlert(@PathVariable String kakaoId, @RequestBody ToggleRequest request, HttpServletRequest servletRequest) {
        validateAdmin(servletRequest);
        adminUseCase.toggleUserAlert(kakaoId, request.isEnabled());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{kakaoId}/reset-send")
    public ResponseEntity<Void> resetSend(@PathVariable String kakaoId, HttpServletRequest request) {
        validateAdmin(request);
        adminUseCase.resetUserLastSentDate(kakaoId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{kakaoId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String kakaoId, HttpServletRequest request) {
        validateAdmin(request);
        adminUseCase.deleteUser(kakaoId);
        return ResponseEntity.noContent().build();
    }

    private void validateAdmin(HttpServletRequest request) {
        if (!isAuthorized(request)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 권한이 필요합니다.");
        }
    }

    private boolean isAuthorized(HttpServletRequest request) {
        String headerKey = request.getHeader("X-Admin-Key");
        if (headerKey != null && headerKey.trim().equals(adminSecretKey.trim())) {
            return true;
        }
        HttpSession session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute(ADMIN_AUTH_SESSION_KEY));
    }
}
