package com.rupeek.hotel.payment.method;

import com.rupeek.hotel.common.exception.ValidationException;
import com.rupeek.hotel.payment.PaymentInstrument;
import com.rupeek.hotel.payment.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

@Component
public class UpiPayment implements PaymentMethod {

    private static final Pattern VPA = Pattern.compile("[\\w.\\-]{2,}@[a-zA-Z]{2,}");

    @Override
    public String type() {
        return "UPI";
    }

    @Override
    public PaymentInstrument prepare(Map<String, String> details) {
        String vpa = Details.required(details, "vpa", "UPI");
        if (!VPA.matcher(vpa).matches()) {
            throw new ValidationException("UPI id must look like name@bank");
        }
        return new PaymentInstrument(type(), "upi " + vpa, vpa);
    }
}
