package com.rupeek.hotel.common.exception;

public class InventoryUnavailableException extends ConflictException {

    public InventoryUnavailableException(String message) {
        super(message);
    }
}
