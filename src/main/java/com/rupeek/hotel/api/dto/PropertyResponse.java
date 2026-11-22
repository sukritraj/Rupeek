package com.rupeek.hotel.api.dto;

import com.rupeek.hotel.property.Property;
import com.rupeek.hotel.property.RoomType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record PropertyResponse(String id, String ownerId, String name, String city, String locality,
                               int starRating, Set<String> amenities, List<RoomTypeResponse> roomTypes) {

    public record RoomTypeResponse(String id, String name, int maxGuests, int totalRooms,
                                   BigDecimal basePrice, Set<String> amenities) {

        static RoomTypeResponse from(RoomType rt) {
            return new RoomTypeResponse(rt.id(), rt.name(), rt.maxGuests(), rt.totalRooms(),
                    rt.basePrice().amount(), rt.amenities());
        }
    }

    public static PropertyResponse from(Property p) {
        return new PropertyResponse(p.id(), p.ownerId(), p.name(), p.address().city(), p.address().locality(),
                p.starRating(), p.amenities(), p.roomTypes().stream().map(RoomTypeResponse::from).toList());
    }
}
