package com.rupeek.hotel.search.filter;

import com.rupeek.hotel.property.Amenities;
import com.rupeek.hotel.search.RoomOffer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/** amenities=wifi,pool - the offer must have all of them (property level or room level). */
@Component
public class AmenitiesFilter implements OfferFilterFactory {

    @Override
    public Optional<Predicate<RoomOffer>> create(Map<String, String> params) {
        return FilterParams.text(params, "amenities").map(raw -> {
            Set<String> required = Amenities.normalise(Arrays.asList(raw.split(",")));
            return offer -> offer.allAmenities().containsAll(required);
        });
    }
}
