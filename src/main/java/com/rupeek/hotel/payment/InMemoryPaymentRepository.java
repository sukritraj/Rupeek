package com.rupeek.hotel.payment;

import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final List<Payment> store = new CopyOnWriteArrayList<>();

    @Override
    public Payment save(Payment payment) {
        store.add(payment);
        return payment;
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String key) {
        return store.stream().filter(p -> key.equals(p.idempotencyKey())).findFirst();
    }

    @Override
    public List<Payment> findByBookingId(String bookingId) {
        return store.stream()
                .filter(p -> p.bookingId().equals(bookingId))
                .sorted(Comparator.comparing(Payment::createdAt))
                .toList();
    }
}
