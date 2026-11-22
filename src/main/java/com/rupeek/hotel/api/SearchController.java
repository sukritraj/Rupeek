package com.rupeek.hotel.api;

import com.rupeek.hotel.api.dto.SearchResponse;
import com.rupeek.hotel.common.DateRange;
import com.rupeek.hotel.search.SearchQuery;
import com.rupeek.hotel.search.SearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/properties")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Anything beyond the core params (minPrice, maxPrice, minStars, amenities, ...) is handed to
     * whichever filter understands it - see OfferFilterFactory.
     */
    @GetMapping("/search")
    public List<SearchResponse> search(
            @RequestParam String city,
            @RequestParam(required = false) String locality,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") int guests,
            @RequestParam Map<String, String> allParams) {
        SearchQuery query = new SearchQuery(city, locality, new DateRange(checkIn, checkOut), guests, allParams);
        return searchService.search(query).stream().map(SearchResponse::from).toList();
    }
}
