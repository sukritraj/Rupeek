package com.rupeek.hotel.inventory;

import com.rupeek.hotel.common.DateRange;
import com.rupeek.hotel.common.KeyedLocks;
import com.rupeek.hotel.property.RoomType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per room type we keep night -> rooms held. Each room type has its own lock, so bookings for
 * different room types never contend while two guests racing for the last room are serialised.
 */
@Component
public class InMemoryInventoryLedger implements InventoryLedger {

    private final Map<String, Map<LocalDate, Integer>> held = new ConcurrentHashMap<>();
    private final KeyedLocks locks = new KeyedLocks();

    @Override
    public int availableRooms(RoomType roomType, DateRange stay) {
        return locks.supply(roomType.id(), () -> roomType.totalRooms() - peakHeld(roomType.id(), stay));
    }

    @Override
    public boolean reserve(RoomType roomType, DateRange stay) {
        return locks.supply(roomType.id(), () -> {
            if (peakHeld(roomType.id(), stay) >= roomType.totalRooms()) {
                return false;
            }
            Map<LocalDate, Integer> nights = held.computeIfAbsent(roomType.id(), k -> new HashMap<>());
            stay.nightDates().forEach(d -> nights.merge(d, 1, Integer::sum));
            return true;
        });
    }

    @Override
    public void release(String roomTypeId, DateRange stay) {
        locks.run(roomTypeId, () -> {
            Map<LocalDate, Integer> nights = held.get(roomTypeId);
            if (nights == null) {
                return;
            }
            stay.nightDates().forEach(d -> nights.computeIfPresent(d, (k, v) -> v > 1 ? v - 1 : null));
        });
    }

    // caller must hold the lock for this room type
    private int peakHeld(String roomTypeId, DateRange stay) {
        Map<LocalDate, Integer> nights = held.get(roomTypeId);
        if (nights == null) {
            return 0;
        }
        return stay.nightDates().mapToInt(d -> nights.getOrDefault(d, 0)).max().orElse(0);
    }
}
