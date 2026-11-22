package com.rupeek.hotel.api.dto;

import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.property.PropertyDraft;
import com.rupeek.hotel.property.PropertyDraft.RoomTypeDraft;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record PropertyRequest(
        @NotBlank String name,
        @NotBlank String city,
        String locality,
        @Min(1) @Max(5) int starRating,
        Set<String> amenities,
        @NotEmpty List<@Valid RoomTypeRequest> roomTypes) {

    public record RoomTypeRequest(
            @NotBlank String name,
            @Min(1) int maxGuests,
            @Min(1) int totalRooms,
            @NotNull @DecimalMin(value = "0.01") BigDecimal basePrice,
            Set<String> amenities) {

        RoomTypeDraft toDraft() {
            return new RoomTypeDraft(name, maxGuests, totalRooms, new Money(basePrice), amenities);
        }
    }

    public PropertyDraft toDraft() {
        return new PropertyDraft(name, city, locality, starRating, amenities,
                roomTypes.stream().map(RoomTypeRequest::toDraft).toList());
    }
}
