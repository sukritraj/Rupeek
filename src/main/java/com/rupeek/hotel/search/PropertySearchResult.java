package com.rupeek.hotel.search;

import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.property.Property;

import java.util.Comparator;
import java.util.List;

public record PropertySearchResult(Property property, List<RoomOffer> offers) {

    public Money startingPrice() {
        return offers.stream().map(RoomOffer::pricePerNight).min(Comparator.naturalOrder()).orElse(Money.ZERO);
    }
}
