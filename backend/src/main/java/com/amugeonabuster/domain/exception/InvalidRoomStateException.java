package com.amugeonabuster.domain.exception;

public class InvalidRoomStateException extends RuntimeException {
    public InvalidRoomStateException(String message) {
        super(message);
    }
}
