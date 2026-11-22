package com.rupeek.hotel.api.dto;

import com.rupeek.hotel.search.PropertySearchResult;
import com.rupeek.hotel.search.RoomOffer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record SearchResponse(String propertyId, String name, String city, String locality, int starRating,
                             Set<String> amenities, BigDecimal startingPricePerNight, List<OfferResponse> offers) {

    public record OfferResponse(String roomTypeId, String roomType, int maxGuests, int roomsLeft,
                                BigDecimal pricePerNight, BigDecimal totalPrice, Set<String> roomAmenities) {

        static OfferResponse from(RoomOffer o) {
            return new OfferResponse(o.roomType().id(), o.roomType().name(), o.roomType().maxGuests(), o.roomsLeft(),
                    o.pricePerNight().amount(), o.totalPrice().amount(), o.roomType().amenities());
        }
    }

    public static SearchResponse from(PropertySearchResult r) {
        var p = r.property();
        return new SearchResponse(p.id(), p.name(), p.address().city(), p.address().locality(), p.starRating(),
                p.amenities(), r.startingPrice().amount(), r.offers().stream().map(OfferResponse::from).toList());
    }
}
