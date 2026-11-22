package com.rupeek.hotel.search.filter;

import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.common.exception.ValidationException;

import java.util.Map;
import java.util.Optional;

/** Small parsing helpers shared by the filter factories so bad input gives a clean 400. */
final class FilterParams {

    private FilterParams() {
    }

    static Optional<String> text(Map<String, String> params, String key) {
        String v = params.get(key);
        return (v == null || v.isBlank()) ? Optional.empty() : Optional.of(v.trim());
    }

    static Optional<Money> money(Map<String, String> params, String key) {
        return text(params, key).map(v -> {
            try {
                return Money.of(v);
            } catch (NumberFormatException e) {
                throw new ValidationException("'" + key + "' must be a number, got: " + v);
            }
        });
    }

    static Optional<Integer> integer(Map<String, String> params, String key) {
        return text(params, key).map(v -> {
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                throw new ValidationException("'" + key + "' must be a whole number, got: " + v);
            }
        });
    }
}
