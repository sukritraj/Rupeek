package com.rupeek.hotel.payment.method;

import com.rupeek.hotel.common.exception.ValidationException;

import java.util.Map;

final class Details {

    private Details() {
    }

    static String required(Map<String, String> details, String key, String method) {
        String v = details == null ? null : details.get(key);
        if (v == null || v.isBlank()) {
            throw new ValidationException(method + " payment needs '" + key + "'");
        }
        return v.trim();
    }
}
