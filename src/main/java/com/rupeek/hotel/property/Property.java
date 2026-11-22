package com.rupeek.hotel.property;

import com.rupeek.hotel.common.Ids;
import com.rupeek.hotel.common.exception.NotFoundException;
import com.rupeek.hotel.common.exception.ValidationException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Property {

    private final String id;
    private final String ownerId;
    private final String name;
    private final Address address;
    private final int starRating;
    private final Set<String> amenities;
    private final List<RoomType> roomTypes;

    public Property(String ownerId, String name, Address address, int starRating,
                    Set<String> amenities, List<RoomType> roomTypes) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Property name is required");
        }
        if (starRating < 1 || starRating > 5) {
            throw new ValidationException("Star rating must be between 1 and 5");
        }
        if (roomTypes == null || roomTypes.isEmpty()) {
            throw new ValidationException("Property '" + name + "' needs at least one room type");
        }
        assertDistinctRoomNames(name, roomTypes);
        this.id = Ids.next("prop");
        this.ownerId = ownerId;
        this.name = name.trim();
        this.address = address;
        this.starRating = starRating;
        this.amenities = Amenities.normalise(amenities);
        this.roomTypes = List.copyOf(roomTypes);
    }

    private static void assertDistinctRoomNames(String property, List<RoomType> roomTypes) {
        Set<String> seen = new HashSet<>();
        for (RoomType rt : roomTypes) {
            if (!seen.add(rt.name().toLowerCase())) {
                throw new ValidationException("Duplicate room type '" + rt.name() + "' in property '" + property + "'");
            }
        }
    }

    public RoomType roomType(String roomTypeId) {
        return roomTypes.stream()
                .filter(rt -> rt.id().equals(roomTypeId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Room type", roomTypeId));
    }

    public String id() {
        return id;
    }

    public String ownerId() {
        return ownerId;
    }

    public String name() {
        return name;
    }

    public Address address() {
        return address;
    }

    public int starRating() {
        return starRating;
    }

    public Set<String> amenities() {
        return amenities;
    }

    public List<RoomType> roomTypes() {
        return roomTypes;
    }
}
