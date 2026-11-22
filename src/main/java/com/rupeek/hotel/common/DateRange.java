package com.rupeek.hotel.common;

import com.rupeek.hotel.common.exception.ValidationException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

/** Half-open stay interval: the guest occupies the room on every night in [checkIn, checkOut). */
public record DateRange(LocalDate checkIn, LocalDate checkOut) {

    public DateRange {
        if (checkIn == null || checkOut == null) {
            throw new ValidationException("Both check-in and check-out dates are required");
        }
        if (!checkIn.isBefore(checkOut)) {
            throw new ValidationException("Check-out must be after check-in");
        }
    }

    public long nights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public Stream<LocalDate> nightDates() {
        return checkIn.datesUntil(checkOut);
    }

    public boolean overlaps(DateRange other) {
        return checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(checkOut);
    }
}
