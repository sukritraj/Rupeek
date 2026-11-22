package com.rupeek.hotel.cancellation;

import com.rupeek.hotel.booking.Booking;
import com.rupeek.hotel.booking.BookingRepository;
import com.rupeek.hotel.booking.BookingService;
import com.rupeek.hotel.booking.BookingStatus;
import com.rupeek.hotel.common.KeyedLocks;
import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.common.exception.InvalidStateTransitionException;
import com.rupeek.hotel.inventory.InventoryLedger;
import com.rupeek.hotel.payment.PaymentService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class CancellationService {

    private final BookingService bookingService;
    private final BookingRepository bookings;
    private final PaymentService paymentService;
    private final InventoryLedger inventory;
    private final CancellationPolicy policy;
    private final KeyedLocks bookingLocks;
    private final Clock clock;

    public CancellationService(BookingService bookingService, BookingRepository bookings, PaymentService paymentService,
                               InventoryLedger inventory, CancellationPolicy policy, KeyedLocks bookingLocks, Clock clock) {
        this.bookingService = bookingService;
        this.bookings = bookings;
        this.paymentService = paymentService;
        this.inventory = inventory;
        this.policy = policy;
        this.bookingLocks = bookingLocks;
        this.clock = clock;
    }

    public CancellationResult cancel(String bookingId) {
        return bookingLocks.supply(bookingId, () -> doCancel(bookingId));
    }

    private CancellationResult doCancel(String bookingId) {
        Booking booking = bookingService.get(bookingId);
        booking.assertCanMoveTo(BookingStatus.CANCELLED);
        if (!LocalDate.now(clock).isBefore(booking.stay().checkOut())) {
            throw new InvalidStateTransitionException("Booking " + bookingId + " belongs to a stay that is already over");
        }

        Instant now = clock.instant();
        Money refund = Money.ZERO;
        if (booking.status() == BookingStatus.CONFIRMED) {
            refund = policy.refundFor(booking, now);
            if (!refund.isZero()) {
                // do the money movement first: if the gateway fails nothing else has changed
                paymentService.refund(bookingId, refund);
            }
        }

        booking.cancel(refund, now);
        bookings.save(booking);
        inventory.release(booking.roomTypeId(), booking.stay());
        return new CancellationResult(booking, refund);
    }
}
