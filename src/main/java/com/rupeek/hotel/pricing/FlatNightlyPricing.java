package com.rupeek.hotel.pricing;

import com.rupeek.hotel.common.DateRange;
import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.property.RoomType;

public class FlatNightlyPricing implements PricingStrategy {

    @Override
    public Money quote(RoomType roomType, DateRange stay, int guests) {
        return roomType.basePrice().times(stay.nights());
    }
}
