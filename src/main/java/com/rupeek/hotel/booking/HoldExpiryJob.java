package com.rupeek.hotel.booking;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HoldExpiryJob {

    private final BookingService bookingService;

    public HoldExpiryJob(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${hotel.booking.expiry-sweep-ms:30000}")
    public void sweep() {
        bookingService.expireStaleHolds();
    }
}
