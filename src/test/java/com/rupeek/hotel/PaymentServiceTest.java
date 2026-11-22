package com.rupeek.hotel;

import com.rupeek.hotel.booking.Booking;
import com.rupeek.hotel.booking.BookingStatus;
import com.rupeek.hotel.common.exception.ConflictException;
import com.rupeek.hotel.common.exception.InvalidStateTransitionException;
import com.rupeek.hotel.common.exception.ValidationException;
import com.rupeek.hotel.payment.Payment;
import com.rupeek.hotel.payment.PaymentCommand;
import com.rupeek.hotel.payment.PaymentStatus;
import com.rupeek.hotel.property.Property;
import com.rupeek.hotel.support.TestWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private final TestWorld world = new TestWorld();
    private Property hotel;
    private Booking booking;

    @BeforeEach
    void setUp() {
        hotel = world.propertyWithRooms(1);
        booking = world.bookingService.book(world.bookingRequest(hotel, world.stay(10, 2), 2));
    }

    @Test
    void successfulPaymentConfirmsBooking() {
        Payment payment = world.paymentService.pay(world.upi(booking.id(), "ria@okbank", null));

        assertThat(payment.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.amount()).isEqualTo(booking.totalPrice());
        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void declinedPaymentKeepsBookingPendingAndAllowsRetry() {
        Payment declined = world.paymentService.pay(world.upi(booking.id(), "willfail@okbank", null));

        assertThat(declined.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);

        Payment retry = world.paymentService.pay(world.upi(booking.id(), "ria@okbank", null));
        assertThat(retry.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void sameIdempotencyKeyDoesNotChargeTwice() {
        Payment first = world.paymentService.pay(world.upi(booking.id(), "ria@okbank", "key-1"));
        Payment replay = world.paymentService.pay(world.upi(booking.id(), "ria@okbank", "key-1"));

        assertThat(replay).isSameAs(first);
        assertThat(world.payments.findByBookingId(booking.id())).hasSize(1);
    }

    @Test
    void idempotencyKeyCannotBeReusedForAnotherBooking() {
        world.paymentService.pay(world.upi(booking.id(), "ria@okbank", "key-1"));
        Booking other = world.bookingService.book(world.bookingRequest(hotel, world.stay(20, 1), 1));

        assertThatThrownBy(() -> world.paymentService.pay(world.upi(other.id(), "ria@okbank", "key-1")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void confirmedBookingCannotBePaidAgain() {
        world.paymentService.pay(world.upi(booking.id(), "ria@okbank", null));

        assertThatThrownBy(() -> world.paymentService.pay(world.upi(booking.id(), "ria@okbank", null)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void paymentAfterHoldExpiredIsRejectedAndInventoryReleased() {
        world.clock.advance(Duration.ofMinutes(20));

        assertThatThrownBy(() -> world.paymentService.pay(world.upi(booking.id(), "ria@okbank", null)))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("expired");
        assertThat(booking.status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(world.payments.findByBookingId(booking.id())).isEmpty();
        world.bookingService.book(world.bookingRequest(hotel, world.stay(10, 2), 2));
    }

    @Test
    void eachMethodValidatesItsOwnDetails() {
        assertThatThrownBy(() -> world.paymentService.pay(world.upi(booking.id(), "not-a-vpa", null)))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> world.paymentService.pay(new PaymentCommand(booking.id(), "CARD",
                Map.of("cardNumber", "4111111111111111", "expiry", "01/20"), null)))
                .isInstanceOf(ValidationException.class).hasMessageContaining("expired");
        assertThatThrownBy(() -> world.paymentService.pay(new PaymentCommand(booking.id(), "WALLET",
                Map.of("provider", "paytm"), null)))
                .isInstanceOf(ValidationException.class).hasMessageContaining("walletId");
        assertThatThrownBy(() -> world.paymentService.pay(new PaymentCommand(booking.id(), "CRYPTO", Map.of(), null)))
                .isInstanceOf(ValidationException.class).hasMessageContaining("Supported");
        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void cardAndWalletPaymentsWork() {
        Payment card = world.paymentService.pay(new PaymentCommand(booking.id(), "card",
                Map.of("cardNumber", "4111 1111 1111 1234", "expiry", "12/30"), null));

        assertThat(card.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(card.instrumentLabel()).isEqualTo("card ending 1234");

        Booking second = world.bookingService.book(world.bookingRequest(hotel, world.stay(20, 1), 1));
        Payment wallet = world.paymentService.pay(new PaymentCommand(second.id(), "WALLET",
                Map.of("provider", "paytm", "walletId", "9999900001"), null));
        assertThat(wallet.status()).isEqualTo(PaymentStatus.SUCCESS);
    }
}
