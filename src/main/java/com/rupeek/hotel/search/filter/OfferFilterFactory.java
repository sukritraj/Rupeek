package com.rupeek.hotel.search.filter;

import com.rupeek.hotel.search.RoomOffer;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * One implementation per filter, registered as a Spring bean. Each looks at the raw request
 * params it understands and returns a predicate - or empty when the caller did not use it.
 * To add a filter: write a new @Component, nothing else changes.
 */
public interface OfferFilterFactory {

    Optional<Predicate<RoomOffer>> create(Map<String, String> params);
}
