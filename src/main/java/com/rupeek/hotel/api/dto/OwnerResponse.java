package com.rupeek.hotel.api.dto;

import com.rupeek.hotel.property.Owner;
import com.rupeek.hotel.property.Property;

import java.util.List;

public record OwnerResponse(String id, String name, String email, List<PropertyResponse> properties) {

    public static OwnerResponse from(Owner owner, List<Property> properties) {
        return new OwnerResponse(owner.id(), owner.name(), owner.email(),
                properties.stream().map(PropertyResponse::from).toList());
    }
}
