package com.rupeek.hotel.payment;

import com.rupeek.hotel.booking.Booking;
import com.rupeek.hotel.booking.BookingRepository;
import com.rupeek.hotel.booking.BookingService;
import com.rupeek.hotel.booking.BookingStatus;
import com.rupeek.hotel.common.KeyedLocks;
import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.common.exception.ConflictException;
import com.rupeek.hotel.common.exception.InvalidStateTransitionException;
import com.rupeek.hotel.common.exception.PaymentException;
import com.rupeek.hotel.payment.PaymentGateway.GatewayResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class PaymentService {

    private final BookingService bookingService;
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final PaymentMethodRegistry methods;
    private final PaymentGateway gateway;
    private final KeyedLocks bookingLocks;
    private final Clock clock;

    public PaymentService(BookingService bookingService, BookingRepository bookings, PaymentRepository payments,
                          PaymentMethodRegistry methods, PaymentGateway gateway, KeyedLocks bookingLocks, Clock clock) {
        this.bookingService = bookingService;
        this.bookings = bookings;
        this.payments = payments;
        this.methods = methods;
        this.gateway = gateway;
        this.bookingLocks = bookingLocks;
        this.clock = clock;
    }

    /**
     * Charges the booking. A successful charge confirms it; a decline leaves it pending so the
     * guest can retry with another method until the hold runs out. Replaying the same
     * idempotency key returns the original result instead of charging again.
     */
    public Payment pay(PaymentCommand command) {
        return bookingLocks.supply(command.bookingId(), () -> charge(command));
    }

    /** Refunds part of the successful payment of a booking. Caller holds the booking lock. */
    public Payment refund(String bookingId, Money amount) {
        Payment payment = payments.findByBookingId(bookingId).stream()
                .filter(Payment::isSuccessful)
                .findFirst()
                .orElseThrow(() -> new PaymentException("No successful payment to refund for booking " + bookingId));
        GatewayResponse response = gateway.refund(payment.gatewayReference(), amount);
        if (!response.success()) {
            throw new PaymentException("Refund failed: " + response.message());
        }
        payment.recordRefund(amount);
        return payment;
    }

    private Payment charge(PaymentCommand command) {
        Optional<Payment> replay = findReplay(command);
        if (replay.isPresent()) {
            return replay.get();
        }

        Booking booking = bookingService.get(command.bookingId());
        assertPayable(booking);

        PaymentInstrument instrument = methods.resolve(command.method()).prepare(command.details());
        GatewayResponse response = gateway.charge(instrument, booking.totalPrice(), booking.id());

        Instant now = clock.instant();
        Payment payment = response.success()
                ? Payment.succeeded(booking.id(), instrument, booking.totalPrice(), command.idempotencyKey(), response.reference(), now)
                : Payment.failed(booking.id(), instrument, booking.totalPrice(), command.idempotencyKey(), response.message(), now);
        payments.save(payment);

        if (response.success()) {
            booking.confirm();
            bookings.save(booking);
        }
        return payment;
    }

    private Optional<Payment> findReplay(PaymentCommand command) {
        String key = command.idempotencyKey();
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        Optional<Payment> existing = payments.findByIdempotencyKey(key);
        existing.ifPresent(p -> {
            if (!p.bookingId().equals(command.bookingId())) {
                throw new ConflictException("Idempotency key already used for a different booking");
            }
        });
        return existing;
    }

    private void assertPayable(Booking booking) {
        if (booking.isHoldExpired(clock.instant())) {
            bookingService.expire(booking);
            throw new InvalidStateTransitionException("The hold on booking " + booking.id() + " has expired");
        }
        if (booking.status() != BookingStatus.PENDING_PAYMENT) {
            throw new InvalidStateTransitionException(
                    "Booking " + booking.id() + " is " + booking.status() + " and cannot be paid");
        }
    }
}
