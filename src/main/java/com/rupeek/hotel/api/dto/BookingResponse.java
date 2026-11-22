package com.rupeek.hotel.api.dto;

import com.rupeek.hotel.booking.Booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(String id, String status, String propertyId, String roomTypeId,
                              LocalDate checkIn, LocalDate checkOut, int guests, String guestName, String guestEmail,
                              BigDecimal totalPrice, BigDecimal refundAmount,
                              Instant holdExpiresAt, Instant cancelledAt) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.id(), b.status().name(), b.propertyId(), b.roomTypeId(),
                b.stay().checkIn(), b.stay().checkOut(), b.guestCount(), b.guest().name(), b.guest().email(),
                b.totalPrice().amount(), b.refundAmount().amount(), b.holdExpiresAt(), b.cancelledAt());
    }
}
