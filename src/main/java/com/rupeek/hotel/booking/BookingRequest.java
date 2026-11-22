package com.rupeek.hotel.booking;

import com.rupeek.hotel.common.DateRange;

public record BookingRequest(String propertyId, String roomTypeId, DateRange stay, int guests, GuestDetails guest) {
}
