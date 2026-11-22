package com.rupeek.hotel.pricing;

import com.rupeek.hotel.common.DateRange;
import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.property.RoomType;

/** Quotes the total for one room over a stay. Swap the bean to change how the platform prices. */
public interface PricingStrategy {

    Money quote(RoomType roomType, DateRange stay, int guests);
}
