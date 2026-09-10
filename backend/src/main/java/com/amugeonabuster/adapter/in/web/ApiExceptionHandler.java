package com.amugeonabuster.adapter.in.web;

import com.amugeonabuster.domain.exception.InvalidRoomStateException;
import com.amugeonabuster.domain.exception.MaxMemberExceededException;
import com.amugeonabuster.domain.exception.UnauthorizedHostException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason() == null ? "요청을 처리할 수 없습니다." : e.getReason()));
    }
    @ExceptionHandler({IllegalArgumentException.class, InvalidRoomStateException.class, MaxMemberExceededException.class})
    public ResponseEntity<Map<String, String>> invalid(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() == null ? "입력 내용을 확인해 주세요." : e.getMessage()));
    }
    @ExceptionHandler(UnauthorizedHostException.class)
    public ResponseEntity<Map<String, String>> forbidden(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
    }
}
