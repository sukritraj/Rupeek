package com.rupeek.hotel.search.filter;

import com.rupeek.hotel.search.RoomOffer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** minStars=4 keeps 4 and 5 star properties. */
@Component
public class StarRatingFilter implements OfferFilterFactory {

    @Override
    public Optional<Predicate<RoomOffer>> create(Map<String, String> params) {
        return FilterParams.integer(params, "minStars")
                .map(min -> offer -> offer.property().starRating() >= min);
    }
}
