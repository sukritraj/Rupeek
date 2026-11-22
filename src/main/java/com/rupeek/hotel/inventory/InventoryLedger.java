package com.rupeek.hotel.inventory;

import com.rupeek.hotel.common.DateRange;
import com.rupeek.hotel.property.RoomType;

/**
 * Tracks how many rooms of a type are held per night. {@link #reserve} is the only place
 * availability is decided, so it must be atomic.
 */
public interface InventoryLedger {

    /** Rooms of this type free on every night of the stay. */
    int availableRooms(RoomType roomType, DateRange stay);

    /** Atomically holds one room for the whole stay; false when any night is sold out. */
    boolean reserve(RoomType roomType, DateRange stay);

    void release(String roomTypeId, DateRange stay);
}
