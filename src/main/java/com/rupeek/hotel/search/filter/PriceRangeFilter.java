package com.rupeek.hotel.search.filter;

import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.common.exception.ValidationException;
import com.rupeek.hotel.search.RoomOffer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** minPrice / maxPrice, both compared against the per-night price. */
@Component
public class PriceRangeFilter implements OfferFilterFactory {

    @Override
    public Optional<Predicate<RoomOffer>> create(Map<String, String> params) {
        Optional<Money> min = FilterParams.money(params, "minPrice");
        Optional<Money> max = FilterParams.money(params, "maxPrice");
        if (min.isEmpty() && max.isEmpty()) {
            return Optional.empty();
        }
        if (min.isPresent() && max.isPresent() && min.get().isGreaterThan(max.get())) {
            throw new ValidationException("minPrice cannot be greater than maxPrice");
        }
        return Optional.of(offer -> {
            Money nightly = offer.pricePerNight();
            return min.map(m -> nightly.compareTo(m) >= 0).orElse(true)
                    && max.map(m -> nightly.compareTo(m) <= 0).orElse(true);
        });
    }
}
