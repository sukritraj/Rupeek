package com.rupeek.hotel.payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findByIdempotencyKey(String key);

    List<Payment> findByBookingId(String bookingId);
}
