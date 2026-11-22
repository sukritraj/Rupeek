package com.rupeek.hotel;

import com.rupeek.hotel.booking.Booking;
import com.rupeek.hotel.booking.BookingStatus;
import com.rupeek.hotel.cancellation.CancellationResult;
import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.common.exception.InvalidStateTransitionException;
import com.rupeek.hotel.common.exception.InventoryUnavailableException;
import com.rupeek.hotel.payment.Payment;
import com.rupeek.hotel.payment.PaymentStatus;
import com.rupeek.hotel.property.Property;
import com.rupeek.hotel.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancellationTest {

    private final TestWorld world = new TestWorld();
    private final Property hotel = world.propertyWithRooms(1);

    private Booking paidBooking(int startInDays) {
        Booking b = world.bookingService.book(world.bookingRequest(hotel, world.stay(startInDays, 2), 2));
        world.paymentService.pay(world.upi(b.id(), "ria@okbank", null));
        return b;
    }

    @Test
    void cancellingWellInAdvanceRefundsEverythingAndFreesTheRoom() {
        Booking booking = paidBooking(10);

        CancellationResult result = world.cancellationService.cancel(booking.id());

        assertThat(result.refund()).isEqualTo(Money.of(10000));
        assertThat(booking.status()).isEqualTo(BookingStatus.CANCELLED);
        Payment payment = world.payments.findByBookingId(booking.id()).get(0);
        assertThat(payment.status()).isEqualTo(PaymentStatus.REFUNDED);
        // inventory is back
        world.bookingService.book(world.bookingRequest(hotel, world.stay(10, 2), 2));
    }

    @Test
    void cancellingInsideTheLastDayGivesNoRefund() {
        Booking booking = paidBooking(1); // check-in tomorrow 14:00, now is 10:00 today -> 28h

        world.clock.advance(Duration.ofHours(10)); // 18h left
        CancellationResult result = world.cancellationService.cancel(booking.id());

        assertThat(result.refund()).isEqualTo(Money.ZERO);
        assertThat(world.payments.findByBookingId(booking.id()).get(0).status()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void cancellingBetween24And48HoursGivesHalf() {
        Booking booking = paidBooking(2); // check-in in 2 days at 14:00 -> 52h out
        world.clock.advance(Duration.ofHours(10)); // 42h out

        assertThat(world.cancellationService.cancel(booking.id()).refund()).isEqualTo(Money.of(5000));
        assertThat(world.payments.findByBookingId(booking.id()).get(0).status()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
    }

    @Test
    void unpaidBookingCancelsWithoutRefundAndReleasesHold() {
        Booking pending = world.bookingService.book(world.bookingRequest(hotel, world.stay(10, 2), 2));

        CancellationResult result = world.cancellationService.cancel(pending.id());

        assertThat(result.refund()).isEqualTo(Money.ZERO);
        assertThat(pending.status()).isEqualTo(BookingStatus.CANCELLED);
        world.bookingService.book(world.bookingRequest(hotel, world.stay(10, 2), 2));
    }

    @Test
    void cannotCancelTwice() {
        Booking booking = paidBooking(10);
        world.cancellationService.cancel(booking.id());

        assertThatThrownBy(() -> world.cancellationService.cancel(booking.id()))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void cannotCancelExpiredBooking() {
        Booking pending = world.bookingService.book(world.bookingRequest(hotel, world.stay(10, 2), 2));
        world.clock.advance(Duration.ofMinutes(30));
        world.bookingService.expireStaleHolds();

        assertThatThrownBy(() -> world.cancellationService.cancel(pending.id()))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void roomIsNotReleasedTwiceWhenCancelRaceIsRetried() {
        Booking first = paidBooking(10);
        world.cancellationService.cancel(first.id());
        Booking second = paidBooking(10); // takes the freed room

        assertThatThrownBy(() -> world.cancellationService.cancel(first.id()))
                .isInstanceOf(InvalidStateTransitionException.class);
        // second guest still owns the room: no one else can book it
        assertThatThrownBy(() -> world.bookingService.book(world.bookingRequest(hotel, world.stay(10, 2), 2)))
                .isInstanceOf(InventoryUnavailableException.class);
        assertThat(second.status()).isEqualTo(BookingStatus.CONFIRMED);
    }
}
