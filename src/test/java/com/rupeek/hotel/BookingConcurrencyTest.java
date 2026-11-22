package com.rupeek.hotel;

import com.rupeek.hotel.booking.Booking;
import com.rupeek.hotel.booking.BookingStatus;
import com.rupeek.hotel.common.exception.InventoryUnavailableException;
import com.rupeek.hotel.payment.PaymentStatus;
import com.rupeek.hotel.property.Property;
import com.rupeek.hotel.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BookingConcurrencyTest {

    private final TestWorld world = new TestWorld();

    @Test
    void racingGuestsGetExactlyAsManyBookingsAsThereAreRooms() throws Exception {
        Property hotel = world.propertyWithRooms(3);
        int guests = 30;
        ExecutorService pool = Executors.newFixedThreadPool(guests);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger sold = new AtomicInteger();
        AtomicInteger turnedAway = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < guests; i++) {
            futures.add(pool.submit(() -> {
                go.await();
                try {
                    world.bookingService.book(world.bookingRequest(hotel, world.stay(10, 2), 2));
                    sold.incrementAndGet();
                } catch (InventoryUnavailableException e) {
                    turnedAway.incrementAndGet();
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(sold.get()).isEqualTo(3);
        assertThat(turnedAway.get()).isEqualTo(27);
    }

    @Test
    void parallelPaymentAttemptsOnOneBookingChargeOnlyOnce() throws Exception {
        Property hotel = world.propertyWithRooms(1);
        Booking booking = world.bookingService.book(world.bookingRequest(hotel, world.stay(10, 2), 2));
        int attempts = 10;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger charged = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            futures.add(pool.submit(() -> {
                go.await();
                try {
                    world.paymentService.pay(world.upi(booking.id(), "ria@okbank", null));
                    charged.incrementAndGet();
                } catch (RuntimeException alreadyPaid) {
                    // expected for every attempt after the first
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(charged.get()).isEqualTo(1);
        assertThat(world.payments.findByBookingId(booking.id()))
                .hasSize(1)
                .allMatch(p -> p.status() == PaymentStatus.SUCCESS);
        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
    }
}
