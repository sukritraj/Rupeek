package com.rupeek.hotel.booking;

/**
 * PENDING_PAYMENT -> CONFIRMED -> CANCELLED
 *        |-> EXPIRED   (hold timed out)
 *        |-> CANCELLED (guest backed out before paying)
 */
public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    EXPIRED;

    public boolean canMoveTo(BookingStatus next) {
        return switch (this) {
            case PENDING_PAYMENT -> next == CONFIRMED || next == CANCELLED || next == EXPIRED;
            case CONFIRMED -> next == CANCELLED;
            case CANCELLED, EXPIRED -> false;
        };
    }

    /** Whether the booking still occupies inventory. */
    public boolean holdsInventory() {
        return this == PENDING_PAYMENT || this == CONFIRMED;
    }
}
