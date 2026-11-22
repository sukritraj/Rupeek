package com.rupeek.hotel.common.exception;

/** Root of all business errors raised by the domain / service layer. */
public abstract class HotelException extends RuntimeException {

    protected HotelException(String message) {
        super(message);
    }
}
