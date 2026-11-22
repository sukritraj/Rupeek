package com.rupeek.hotel;

import com.rupeek.hotel.booking.Booking;
import com.rupeek.hotel.booking.BookingStatus;
import com.rupeek.hotel.booking.GuestDetails;
import com.rupeek.hotel.common.DateRange;
import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.common.exception.InvalidStateTransitionException;
import com.rupeek.hotel.common.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainModelTest {

    private static final Instant NOW = Instant.parse("2026-06-10T10:00:00Z");

    private Booking newBooking() {
        return Booking.hold("prop-1", "room-1", new GuestDetails("Ria", "ria@example.com"),
                new DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3)), 2, Money.of(10000),
                NOW, NOW.plusSeconds(900));
    }

    @Test
    void dateRangeRejectsEmptyOrInvertedStay() {
        LocalDate d = LocalDate.of(2026, 7, 1);
        assertThatThrownBy(() -> new DateRange(d, d)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new DateRange(d, d.minusDays(1))).isInstanceOf(ValidationException.class);
    }

    @Test
    void checkoutDayIsNotAnOccupiedNight() {
        DateRange a = new DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3));
        DateRange backToBack = new DateRange(LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 5));
        DateRange overlapping = new DateRange(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 4));

        assertThat(a.nights()).isEqualTo(2);
        assertThat(a.overlaps(backToBack)).isFalse();
        assertThat(a.overlaps(overlapping)).isTrue();
    }

    @Test
    void newBookingWaitsForPayment() {
        assertThat(newBooking().status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void confirmedBookingCanBeCancelledButNotConfirmedAgain() {
        Booking b = newBooking();
        b.confirm();
        assertThatThrownBy(b::confirm).isInstanceOf(InvalidStateTransitionException.class);

        b.cancel(Money.of(5000), NOW);
        assertThat(b.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(b.refundAmount()).isEqualTo(Money.of(5000));
    }

    @Test
    void terminalStatesAreFinal() {
        Booking expired = newBooking();
        expired.expire();
        assertThatThrownBy(expired::confirm).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> expired.cancel(Money.ZERO, NOW)).isInstanceOf(InvalidStateTransitionException.class);

        Booking cancelled = newBooking();
        cancelled.cancel(Money.ZERO, NOW);
        assertThatThrownBy(cancelled::confirm).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void holdOnlyExpiresWhilePending() {
        Booking b = newBooking();
        assertThat(b.isHoldExpired(NOW.plusSeconds(899))).isFalse();
        assertThat(b.isHoldExpired(NOW.plusSeconds(900))).isTrue();

        b.confirm();
        assertThat(b.isHoldExpired(NOW.plusSeconds(5000))).isFalse();
    }

    @Test
    void moneyKeepsTwoDecimalsAndRefusesNegatives() {
        assertThat(Money.of("100").percent(33).amount().toPlainString()).isEqualTo("33.00");
        assertThat(Money.of("10").dividedBy(3).amount().toPlainString()).isEqualTo("3.33");
        assertThatThrownBy(() -> Money.of(10).minus(Money.of(11))).isInstanceOf(ValidationException.class);
    }
}
