package com.rupeek.hotel.api.dto;

import com.rupeek.hotel.booking.BookingRequest;
import com.rupeek.hotel.booking.GuestDetails;
import com.rupeek.hotel.common.DateRange;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateBookingRequest(
        @NotBlank String propertyId,
        @NotBlank String roomTypeId,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut,
        @Min(1) int guests,
        @NotBlank String guestName,
        @NotBlank @Email String guestEmail) {

    public BookingRequest toCommand() {
        return new BookingRequest(propertyId, roomTypeId, new DateRange(checkIn, checkOut), guests,
                new GuestDetails(guestName, guestEmail));
    }
}
