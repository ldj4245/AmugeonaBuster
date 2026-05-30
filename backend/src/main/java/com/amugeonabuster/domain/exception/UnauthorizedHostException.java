package com.amugeonabuster.domain.exception;

public class UnauthorizedHostException extends RuntimeException {
    public UnauthorizedHostException(String message) {
        super(message);
    }
}
