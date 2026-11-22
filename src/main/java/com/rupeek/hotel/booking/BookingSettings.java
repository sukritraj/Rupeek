package com.rupeek.hotel.booking;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "hotel.booking")
public record BookingSettings(@DefaultValue("15") int holdMinutes,
                              @DefaultValue("30") int maxNights) {
}
