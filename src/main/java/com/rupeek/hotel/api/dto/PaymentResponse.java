package com.rupeek.hotel.api.dto;

import com.rupeek.hotel.booking.Booking;
import com.rupeek.hotel.payment.Payment;

import java.math.BigDecimal;

public record PaymentResponse(String paymentId, String paymentStatus, String method, String instrument,
                              BigDecimal amount, String failureReason, String bookingId, String bookingStatus) {

    public static PaymentResponse from(Payment p, Booking b) {
        return new PaymentResponse(p.id(), p.status().name(), p.methodType(), p.instrumentLabel(),
                p.amount().amount(), p.failureReason(), b.id(), b.status().name());
    }
}
