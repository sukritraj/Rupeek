package com.rupeek.hotel.search;

import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.property.Property;
import com.rupeek.hotel.property.RoomType;

import java.util.HashSet;
import java.util.Set;

/** A bookable room type for the searched stay, with its quoted price. */
public record RoomOffer(Property property, RoomType roomType, Money totalPrice, long nights, int roomsLeft) {

    public Money pricePerNight() {
        return totalPrice.dividedBy(nights);
    }

    /** Amenities a guest would get: what the property offers plus what the room adds. */
    public Set<String> allAmenities() {
        Set<String> all = new HashSet<>(property.amenities());
        all.addAll(roomType.amenities());
        return all;
    }
}
