package com.rupeek.hotel.common.exception;

public class InvalidStateTransitionException extends ConflictException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
