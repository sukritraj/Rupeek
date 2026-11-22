package com.rupeek.hotel.search;

import com.rupeek.hotel.common.DateRange;

import java.util.Map;

/**
 * Core criteria are typed; everything optional travels in {@code filterParams} so that a new
 * filter never needs a change to this class or to the search service.
 */
public record SearchQuery(String city, String locality, DateRange stay, int guests, Map<String, String> filterParams) {

    public SearchQuery {
        filterParams = filterParams == null ? Map.of() : Map.copyOf(filterParams);
    }
}
