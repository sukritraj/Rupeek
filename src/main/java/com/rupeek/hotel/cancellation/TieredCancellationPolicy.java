package com.rupeek.hotel.cancellation;

import com.rupeek.hotel.booking.Booking;
import com.rupeek.hotel.common.Money;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * The earlier the guest cancels, the more they get back. Tiers are "at least N hours before
 * check-in -> P% refund"; anything later than every tier gets nothing.
 */
public class TieredCancellationPolicy implements CancellationPolicy {

    public record Tier(long minHoursBeforeCheckIn, int refundPercent) {
    }

    private static final LocalTime CHECK_IN_TIME = LocalTime.of(14, 0);

    private final List<Tier> tiers;
    private final Clock clock;

    public TieredCancellationPolicy(List<Tier> tiers, Clock clock) {
        this.tiers = tiers.stream()
                .sorted(Comparator.comparingLong(Tier::minHoursBeforeCheckIn).reversed())
                .toList();
        this.clock = clock;
    }

    @Override
    public Money refundFor(Booking booking, Instant cancelledAt) {
        Instant checkIn = booking.stay().checkIn().atTime(CHECK_IN_TIME).atZone(clock.getZone()).toInstant();
        long hoursLeft = Duration.between(cancelledAt, checkIn).toHours();
        return tiers.stream()
                .filter(t -> hoursLeft >= t.minHoursBeforeCheckIn())
                .findFirst()
                .map(t -> booking.totalPrice().percent(t.refundPercent()))
                .orElse(Money.ZERO);
    }
}
