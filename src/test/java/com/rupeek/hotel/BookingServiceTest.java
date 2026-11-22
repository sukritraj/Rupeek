package com.rupeek.hotel;

import com.rupeek.hotel.booking.Booking;
import com.rupeek.hotel.booking.BookingStatus;
import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.common.exception.InventoryUnavailableException;
import com.rupeek.hotel.common.exception.NotFoundException;
import com.rupeek.hotel.common.exception.ValidationException;
import com.rupeek.hotel.pricing.WeekendSurchargePricing;
import com.rupeek.hotel.property.Property;
import com.rupeek.hotel.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingServiceTest {

    private final TestWorld world = new TestWorld();

    @Test
    void bookingStartsPendingWithQuotedPrice() {
        Property hotel = world.propertyWithRooms(2);

        Booking booking = world.bookingService.book(world.bookingRequest(hotel, world.stay(10, 3), 2));

        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.totalPrice()).isEqualTo(Money.of(15000));
        assertThat(booking.holdExpiresAt()).isEqualTo(world.clock.instant().plus(Duration.ofMinutes(15)));
    }

    @Test
    void lastRoomCannotBeBookedTwiceForOverlappingDates() {
        Property hotel = world.propertyWithRooms(1);
        world.bookingService.book(world.bookingRequest(hotel, world.stay(10, 3), 2));

        assertThatThrownBy(() -> world.bookingService.book(world.bookingRequest(hotel, world.stay(12, 2), 2)))
                .isInstanceOf(InventoryUnavailableException.class);
        // the day the first guest checks out is free again
        world.bookingService.book(world.bookingRequest(hotel, world.stay(13, 2), 2));
    }

    @Test
    void rejectsMoreGuestsThanTheRoomSleeps() {
        Property hotel = world.propertyWithRooms(1);
        assertThatThrownBy(() -> world.bookingService.book(world.bookingRequest(hotel, world.stay(5, 1), 3)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("sleeps at most 2");
    }

    @Test
    void rejectsPastAndOverlongStays() {
        Property hotel = world.propertyWithRooms(1);
        assertThatThrownBy(() -> world.bookingService.book(world.bookingRequest(hotel, world.stay(-1, 2), 1)))
                .isInstanceOf(ValidationException.class).hasMessageContaining("past");
        assertThatThrownBy(() -> world.bookingService.book(world.bookingRequest(hotel, world.stay(5, 31), 1)))
                .isInstanceOf(ValidationException.class).hasMessageContaining("30 nights");
    }

    @Test
    void unknownPropertyOrRoomTypeIsNotFound() {
        Property hotel = world.propertyWithRooms(1);
        var request = world.bookingRequest(hotel, world.stay(5, 1), 1);

        assertThatThrownBy(() -> world.bookingService.book(new com.rupeek.hotel.booking.BookingRequest(
                "nope", request.roomTypeId(), request.stay(), 1, request.guest())))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> world.bookingService.book(new com.rupeek.hotel.booking.BookingRequest(
                hotel.id(), "nope", request.stay(), 1, request.guest())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void staleHoldsAreReleasedBackToInventory() {
        Property hotel = world.propertyWithRooms(1);
        Booking unpaid = world.bookingService.book(world.bookingRequest(hotel, world.stay(10, 2), 2));

        world.clock.advance(Duration.ofMinutes(16));
        int released = world.bookingService.expireStaleHolds();

        assertThat(released).isEqualTo(1);
        assertThat(unpaid.status()).isEqualTo(BookingStatus.EXPIRED);
        // room is bookable again
        world.bookingService.book(world.bookingRequest(hotel, world.stay(10, 2), 2));
    }

    @Test
    void weekendNightsCostMoreWithSurchargeStrategy() {
        TestWorld weekendWorld = new TestWorld(new WeekendSurchargePricing(20));
        Property hotel = weekendWorld.propertyWithRooms(1);
        // 2026-06-10 is a Wednesday; find the next Friday and book Fri + Sat nights
        int daysToFriday = (5 - weekendWorld.today().getDayOfWeek().getValue() + 7) % 7;
        if (daysToFriday == 0) {
            daysToFriday = 7;
        }

        Booking booking = weekendWorld.bookingService.book(
                weekendWorld.bookingRequest(hotel, weekendWorld.stay(daysToFriday, 2), 2));

        assertThat(booking.totalPrice()).isEqualTo(Money.of(12000)); // 2 x (5000 + 20%)
    }
}
