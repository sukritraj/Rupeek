package com.rupeek.hotel.config;

import com.rupeek.hotel.booking.BookingSettings;
import com.rupeek.hotel.cancellation.CancellationPolicy;
import com.rupeek.hotel.cancellation.TieredCancellationPolicy;
import com.rupeek.hotel.cancellation.TieredCancellationPolicy.Tier;
import com.rupeek.hotel.common.KeyedLocks;
import com.rupeek.hotel.pricing.FlatNightlyPricing;
import com.rupeek.hotel.pricing.PricingStrategy;
import com.rupeek.hotel.pricing.WeekendSurchargePricing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.util.List;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(BookingSettings.class)
public class HotelConfig {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public KeyedLocks bookingLocks() {
        return new KeyedLocks();
    }

    @Bean
    @ConditionalOnProperty(name = "hotel.pricing.strategy", havingValue = "weekend-surcharge")
    public PricingStrategy weekendPricing(@Value("${hotel.pricing.weekend-surcharge-percent:20}") int percent) {
        return new WeekendSurchargePricing(percent);
    }

    @Bean
    @ConditionalOnProperty(name = "hotel.pricing.strategy", havingValue = "base", matchIfMissing = true)
    public PricingStrategy flatPricing() {
        return new FlatNightlyPricing();
    }

    /** Full refund 48h+ out, half refund 24h+ out, nothing after that. */
    @Bean
    public CancellationPolicy cancellationPolicy(Clock clock) {
        return new TieredCancellationPolicy(List.of(new Tier(48, 100), new Tier(24, 50)), clock);
    }
}
