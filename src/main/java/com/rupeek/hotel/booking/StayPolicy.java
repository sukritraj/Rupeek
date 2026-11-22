package com.rupeek.hotel.booking;

import com.rupeek.hotel.common.DateRange;
import com.rupeek.hotel.common.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/** Rules about which stays the platform accepts at all (search and booking share them). */
@Component
public class StayPolicy {

    private final Clock clock;
    private final BookingSettings settings;

    public StayPolicy(Clock clock, BookingSettings settings) {
        this.clock = clock;
        this.settings = settings;
    }

    public void check(DateRange stay, int guests) {
        if (stay.checkIn().isBefore(LocalDate.now(clock))) {
            throw new ValidationException("Check-in date cannot be in the past");
        }
        if (stay.nights() > settings.maxNights()) {
            throw new ValidationException("Stays longer than " + settings.maxNights() + " nights are not supported");
        }
        if (guests < 1) {
            throw new ValidationException("At least one guest is required");
        }
    }
}
