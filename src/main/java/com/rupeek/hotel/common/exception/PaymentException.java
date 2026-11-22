package com.rupeek.hotel.common.exception;

/** Raised when the payment provider itself misbehaves (e.g. a refund cannot be issued). */
public class PaymentException extends HotelException {

    public PaymentException(String message) {
        super(message);
    }
}
