package com.rupeek.hotel.search;

import com.rupeek.hotel.booking.StayPolicy;
import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.common.exception.ValidationException;
import com.rupeek.hotel.inventory.InventoryLedger;
import com.rupeek.hotel.pricing.PricingStrategy;
import com.rupeek.hotel.property.Property;
import com.rupeek.hotel.property.PropertyRepository;
import com.rupeek.hotel.property.RoomType;
import com.rupeek.hotel.search.filter.OfferFilterFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class SearchService {

    private final PropertyRepository properties;
    private final InventoryLedger inventory;
    private final PricingStrategy pricing;
    private final StayPolicy stayPolicy;
    private final List<OfferFilterFactory> filterFactories;

    public SearchService(PropertyRepository properties, InventoryLedger inventory, PricingStrategy pricing,
                         StayPolicy stayPolicy, List<OfferFilterFactory> filterFactories) {
        this.properties = properties;
        this.inventory = inventory;
        this.pricing = pricing;
        this.stayPolicy = stayPolicy;
        this.filterFactories = filterFactories;
    }

    public List<PropertySearchResult> search(SearchQuery query) {
        if (query.city() == null || query.city().isBlank()) {
            throw new ValidationException("City is required");
        }
        stayPolicy.check(query.stay(), query.guests());

        Predicate<RoomOffer> filter = buildFilter(query);

        return properties.findByLocation(query.city(), query.locality()).stream()
                .map(property -> toResult(property, query, filter))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(PropertySearchResult::startingPrice))
                .toList();
    }

    private Predicate<RoomOffer> buildFilter(SearchQuery query) {
        return filterFactories.stream()
                .map(f -> f.create(query.filterParams()))
                .flatMap(Optional::stream)
                .reduce(offer -> true, Predicate::and);
    }

    private Optional<PropertySearchResult> toResult(Property property, SearchQuery query, Predicate<RoomOffer> filter) {
        List<RoomOffer> offers = property.roomTypes().stream()
                .filter(rt -> rt.maxGuests() >= query.guests())
                .map(rt -> toOffer(property, rt, query))
                .flatMap(Optional::stream)
                .filter(filter)
                .toList();
        return offers.isEmpty() ? Optional.empty() : Optional.of(new PropertySearchResult(property, offers));
    }

    private Optional<RoomOffer> toOffer(Property property, RoomType roomType, SearchQuery query) {
        int left = inventory.availableRooms(roomType, query.stay());
        if (left <= 0) {
            return Optional.empty();
        }
        Money total = pricing.quote(roomType, query.stay(), query.guests());
        return Optional.of(new RoomOffer(property, roomType, total, query.stay().nights(), left));
    }
}
