package com.rupeek.hotel.api.dto;

import com.rupeek.hotel.cancellation.CancellationResult;

import java.math.BigDecimal;

public record CancellationResponse(BookingResponse booking, BigDecimal refund) {

    public static CancellationResponse from(CancellationResult r) {
        return new CancellationResponse(BookingResponse.from(r.booking()), r.refund().amount());
    }
}
