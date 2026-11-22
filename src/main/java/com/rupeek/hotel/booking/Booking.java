package com.rupeek.hotel.booking;

import com.rupeek.hotel.common.DateRange;
import com.rupeek.hotel.common.Ids;
import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.common.exception.InvalidStateTransitionException;

import java.time.Instant;

public class Booking {

    private final String id;
    private final String propertyId;
    private final String roomTypeId;
    private final GuestDetails guest;
    private final DateRange stay;
    private final int guestCount;
    private final Money totalPrice;
    private final Instant createdAt;
    private final Instant holdExpiresAt;

    // written under the booking lock, read from anywhere
    private volatile BookingStatus status = BookingStatus.PENDING_PAYMENT;
    private volatile Money refundAmount = Money.ZERO;
    private volatile Instant cancelledAt;

    private Booking(String propertyId, String roomTypeId, GuestDetails guest, DateRange stay,
                    int guestCount, Money totalPrice, Instant now, Instant holdExpiresAt) {
        this.id = Ids.next("bkg");
        this.propertyId = propertyId;
        this.roomTypeId = roomTypeId;
        this.guest = guest;
        this.stay = stay;
        this.guestCount = guestCount;
        this.totalPrice = totalPrice;
        this.createdAt = now;
        this.holdExpiresAt = holdExpiresAt;
    }

    public static Booking hold(String propertyId, String roomTypeId, GuestDetails guest, DateRange stay,
                               int guestCount, Money totalPrice, Instant now, Instant holdExpiresAt) {
        return new Booking(propertyId, roomTypeId, guest, stay, guestCount, totalPrice, now, holdExpiresAt);
    }

    public void confirm() {
        moveTo(BookingStatus.CONFIRMED);
    }

    public void expire() {
        moveTo(BookingStatus.EXPIRED);
    }

    public void cancel(Money refund, Instant at) {
        moveTo(BookingStatus.CANCELLED);
        this.refundAmount = refund;
        this.cancelledAt = at;
    }

    public boolean isHoldExpired(Instant now) {
        return status == BookingStatus.PENDING_PAYMENT && !now.isBefore(holdExpiresAt);
    }

    public void assertCanMoveTo(BookingStatus next) {
        if (!status.canMoveTo(next)) {
            throw new InvalidStateTransitionException(
                    "Booking " + id + " is " + status + " and cannot become " + next);
        }
    }

    private void moveTo(BookingStatus next) {
        assertCanMoveTo(next);
        this.status = next;
    }

    public String id() {
        return id;
    }

    public String propertyId() {
        return propertyId;
    }

    public String roomTypeId() {
        return roomTypeId;
    }

    public GuestDetails guest() {
        return guest;
    }

    public DateRange stay() {
        return stay;
    }

    public int guestCount() {
        return guestCount;
    }

    public Money totalPrice() {
        return totalPrice;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant holdExpiresAt() {
        return holdExpiresAt;
    }

    public BookingStatus status() {
        return status;
    }

    public Money refundAmount() {
        return refundAmount;
    }

    public Instant cancelledAt() {
        return cancelledAt;
    }
}
