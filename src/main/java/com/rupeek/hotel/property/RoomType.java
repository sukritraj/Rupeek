package com.rupeek.hotel.property;

import com.rupeek.hotel.common.Ids;
import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.common.exception.ValidationException;

import java.util.Set;

/** A category of room in a property (e.g. "Deluxe King") along with how many of them exist. */
public final class RoomType {

    private final String id;
    private final String name;
    private final int maxGuests;
    private final int totalRooms;
    private final Money basePrice;
    private final Set<String> amenities;

    public RoomType(String name, int maxGuests, int totalRooms, Money basePrice, Set<String> amenities) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Room type name is required");
        }
        if (maxGuests < 1) {
            throw new ValidationException("Room type '" + name + "' must sleep at least one guest");
        }
        if (totalRooms < 1) {
            throw new ValidationException("Room type '" + name + "' must have at least one room");
        }
        if (basePrice == null || basePrice.isZero()) {
            throw new ValidationException("Room type '" + name + "' needs a base price above zero");
        }
        this.id = Ids.next("room");
        this.name = name.trim();
        this.maxGuests = maxGuests;
        this.totalRooms = totalRooms;
        this.basePrice = basePrice;
        this.amenities = Amenities.normalise(amenities);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int maxGuests() {
        return maxGuests;
    }

    public int totalRooms() {
        return totalRooms;
    }

    public Money basePrice() {
        return basePrice;
    }

    public Set<String> amenities() {
        return amenities;
    }
}
