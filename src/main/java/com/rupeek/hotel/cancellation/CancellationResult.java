package com.rupeek.hotel.cancellation;

import com.rupeek.hotel.booking.Booking;
import com.rupeek.hotel.common.Money;

public record CancellationResult(Booking booking, Money refund) {
}
