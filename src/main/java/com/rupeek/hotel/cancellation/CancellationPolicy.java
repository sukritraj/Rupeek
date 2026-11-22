package com.rupeek.hotel.cancellation;

import com.rupeek.hotel.booking.Booking;
import com.rupeek.hotel.common.Money;

import java.time.Instant;

/** Decides how much of a confirmed booking's payment goes back to the guest. */
public interface CancellationPolicy {

    Money refundFor(Booking booking, Instant cancelledAt);
}
