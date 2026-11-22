package com.rupeek.hotel.payment.method;

import com.rupeek.hotel.payment.PaymentInstrument;
import com.rupeek.hotel.payment.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WalletPayment implements PaymentMethod {

    @Override
    public String type() {
        return "WALLET";
    }

    @Override
    public PaymentInstrument prepare(Map<String, String> details) {
        String provider = Details.required(details, "provider", "WALLET");
        String walletId = Details.required(details, "walletId", "WALLET");
        return new PaymentInstrument(type(), provider + " wallet " + walletId, walletId);
    }
}
