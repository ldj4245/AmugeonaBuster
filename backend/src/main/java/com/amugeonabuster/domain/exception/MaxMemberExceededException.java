package com.amugeonabuster.domain.exception;

public class MaxMemberExceededException extends RuntimeException {
    public MaxMemberExceededException(String message) {
        super(message);
    }
}
