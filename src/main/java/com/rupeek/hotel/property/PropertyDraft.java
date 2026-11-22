package com.rupeek.hotel.property;

import com.rupeek.hotel.common.Money;

import java.util.List;
import java.util.Set;

/** What an owner submits when onboarding a property. */
public record PropertyDraft(String name, String city, String locality, int starRating,
                            Set<String> amenities, List<RoomTypeDraft> roomTypes) {

    public record RoomTypeDraft(String name, int maxGuests, int totalRooms, Money basePrice, Set<String> amenities) {
    }
}
