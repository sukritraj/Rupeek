package com.rupeek.hotel.property;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository {

    Property save(Property property);

    Optional<Property> findById(String id);

    List<Property> findByOwner(String ownerId);

    /** Locality is optional; when null/blank only the city is matched. */
    List<Property> findByLocation(String city, String locality);
}
