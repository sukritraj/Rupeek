package com.rupeek.hotel.property;

import com.rupeek.hotel.common.exception.ValidationException;

public record Address(String city, String locality) {

    public Address {
        if (city == null || city.isBlank()) {
            throw new ValidationException("City is required");
        }
        city = city.trim();
        locality = (locality == null || locality.isBlank()) ? null : locality.trim();
    }

    public boolean isIn(String queryCity, String queryLocality) {
        if (!city.equalsIgnoreCase(queryCity.trim())) {
            return false;
        }
        return queryLocality == null || queryLocality.isBlank()
                || (locality != null && locality.equalsIgnoreCase(queryLocality.trim()));
    }
}
