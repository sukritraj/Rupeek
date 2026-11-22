package com.rupeek.hotel.payment;

import com.rupeek.hotel.common.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

@Component
public class PaymentMethodRegistry {

    private final Map<String, PaymentMethod> byType = new HashMap<>();

    public PaymentMethodRegistry(List<PaymentMethod> methods) {
        for (PaymentMethod method : methods) {
            String key = method.type().toUpperCase(Locale.ROOT);
            if (byType.putIfAbsent(key, method) != null) {
                throw new IllegalStateException("Two payment methods registered for " + key);
            }
        }
    }

    public PaymentMethod resolve(String type) {
        PaymentMethod method = type == null ? null : byType.get(type.trim().toUpperCase(Locale.ROOT));
        if (method == null) {
            throw new ValidationException("Unsupported payment method '" + type + "'. Supported: "
                    + new TreeSet<>(byType.keySet()));
        }
        return method;
    }
}
