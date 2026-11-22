package com.rupeek.hotel.booking;

import com.rupeek.hotel.common.KeyedLocks;
import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.common.exception.InventoryUnavailableException;
import com.rupeek.hotel.common.exception.NotFoundException;
import com.rupeek.hotel.common.exception.ValidationException;
import com.rupeek.hotel.inventory.InventoryLedger;
import com.rupeek.hotel.pricing.PricingStrategy;
import com.rupeek.hotel.property.Property;
import com.rupeek.hotel.property.PropertyRepository;
import com.rupeek.hotel.property.RoomType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final PropertyRepository properties;
    private final BookingRepository bookings;
    private final InventoryLedger inventory;
    private final PricingStrategy pricing;
    private final StayPolicy stayPolicy;
    private final KeyedLocks bookingLocks;
    private final BookingSettings settings;
    private final Clock clock;

    public BookingService(PropertyRepository properties, BookingRepository bookings, InventoryLedger inventory,
                          PricingStrategy pricing, StayPolicy stayPolicy, KeyedLocks bookingLocks,
                          BookingSettings settings, Clock clock) {
        this.properties = properties;
        this.bookings = bookings;
        this.inventory = inventory;
        this.pricing = pricing;
        this.stayPolicy = stayPolicy;
        this.bookingLocks = bookingLocks;
        this.settings = settings;
        this.clock = clock;
    }

    /** Places a hold on one room and returns a booking waiting for payment. */
    public Booking book(BookingRequest request) {
        stayPolicy.check(request.stay(), request.guests());
        Property property = properties.findById(request.propertyId())
                .orElseThrow(() -> new NotFoundException("Property", request.propertyId()));
        RoomType roomType = property.roomType(request.roomTypeId());
        if (request.guests() > roomType.maxGuests()) {
            throw new ValidationException("Room type '" + roomType.name() + "' sleeps at most "
                    + roomType.maxGuests() + " guests");
        }

        Money price = pricing.quote(roomType, request.stay(), request.guests());

        if (!inventory.reserve(roomType, request.stay())) {
            throw new InventoryUnavailableException("No '" + roomType.name() + "' rooms left for "
                    + request.stay().checkIn() + " to " + request.stay().checkOut());
        }
        try {
            Instant now = clock.instant();
            Booking booking = Booking.hold(property.id(), roomType.id(), request.guest(), request.stay(),
                    request.guests(), price, now, now.plus(Duration.ofMinutes(settings.holdMinutes())));
            return bookings.save(booking);
        } catch (RuntimeException e) {
            inventory.release(roomType.id(), request.stay());
            throw e;
        }
    }

    public Booking get(String bookingId) {
        return bookings.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking", bookingId));
    }

    /** Gives back the room of a booking that timed out. Caller must hold the booking lock. */
    public void expire(Booking booking) {
        booking.expire();
        bookings.save(booking);
        inventory.release(booking.roomTypeId(), booking.stay());
        log.info("Hold expired for booking {}", booking.id());
    }

    /** Sweeps unpaid bookings whose hold ran out; returns how many were released. */
    public int expireStaleHolds() {
        Instant now = clock.instant();
        int released = 0;
        for (Booking candidate : bookings.findByStatus(BookingStatus.PENDING_PAYMENT)) {
            if (!candidate.isHoldExpired(now)) {
                continue;
            }
            released += bookingLocks.supply(candidate.id(), () -> {
                // re-check: a payment may have landed while we waited for the lock
                if (candidate.isHoldExpired(now)) {
                    expire(candidate);
                    return 1;
                }
                return 0;
            });
        }
        return released;
    }
}
