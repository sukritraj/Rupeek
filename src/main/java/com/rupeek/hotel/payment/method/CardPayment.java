package com.rupeek.hotel.payment.method;

import com.rupeek.hotel.common.exception.ValidationException;
import com.rupeek.hotel.payment.PaymentInstrument;
import com.rupeek.hotel.payment.PaymentMethod;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class CardPayment implements PaymentMethod {

    private static final Pattern NUMBER = Pattern.compile("\\d{12,19}");
    private static final Pattern EXPIRY = Pattern.compile("(0[1-9]|1[0-2])/(\\d{2})");

    private final Clock clock;

    public CardPayment(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String type() {
        return "CARD";
    }

    @Override
    public PaymentInstrument prepare(Map<String, String> details) {
        String number = Details.required(details, "cardNumber", "CARD").replace(" ", "");
        String expiry = Details.required(details, "expiry", "CARD");
        if (!NUMBER.matcher(number).matches()) {
            throw new ValidationException("Card number must be 12-19 digits");
        }
        var m = EXPIRY.matcher(expiry);
        if (!m.matches()) {
            throw new ValidationException("Card expiry must look like MM/YY");
        }
        YearMonth expires = YearMonth.of(2000 + Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
        if (expires.isBefore(YearMonth.now(clock))) {
            throw new ValidationException("Card has expired");
        }
        return new PaymentInstrument(type(), "card ending " + number.substring(number.length() - 4), number);
    }
}
