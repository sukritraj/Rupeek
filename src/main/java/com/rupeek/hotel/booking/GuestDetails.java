package com.rupeek.hotel.booking;

import com.rupeek.hotel.common.exception.ValidationException;

public record GuestDetails(String name, String email) {

    public GuestDetails {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Guest name is required");
        }
        if (email == null || !email.contains("@")) {
            throw new ValidationException("A valid guest email is required");
        }
        name = name.trim();
        email = email.trim();
    }
}
