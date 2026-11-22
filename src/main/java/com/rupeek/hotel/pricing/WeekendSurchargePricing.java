package com.rupeek.hotel.pricing;

import com.rupeek.hotel.common.DateRange;
import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.property.RoomType;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Friday and Saturday nights cost an extra percentage on top of the base rate. */
public class WeekendSurchargePricing implements PricingStrategy {

    private final int surchargePercent;

    public WeekendSurchargePricing(int surchargePercent) {
        this.surchargePercent = surchargePercent;
    }

    @Override
    public Money quote(RoomType roomType, DateRange stay, int guests) {
        return stay.nightDates()
                .map(night -> nightlyRate(roomType, night))
                .reduce(Money.ZERO, Money::plus);
    }

    private Money nightlyRate(RoomType roomType, LocalDate night) {
        Money base = roomType.basePrice();
        return isWeekendNight(night) ? base.plus(base.percent(surchargePercent)) : base;
    }

    private boolean isWeekendNight(LocalDate night) {
        DayOfWeek day = night.getDayOfWeek();
        return day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY;
    }
}
