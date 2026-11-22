package com.rupeek.hotel.common.exception;

public class NotFoundException extends HotelException {

    public NotFoundException(String resource, String id) {
        super(resource + " not found: " + id);
    }
}
