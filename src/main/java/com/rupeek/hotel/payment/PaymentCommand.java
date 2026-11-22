package com.rupeek.hotel.payment;

import java.util.Map;

public record PaymentCommand(String bookingId, String method, Map<String, String> details, String idempotencyKey) {
}
