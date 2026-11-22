package com.rupeek.hotel.payment;

import com.rupeek.hotel.common.Ids;
import com.rupeek.hotel.common.Money;
import org.springframework.stereotype.Component;

/**
 * Stand-in for a real processor. Approves everything except credentials that end in "0000"
 * or contain "fail", which lets callers exercise the decline path deterministically.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public GatewayResponse charge(PaymentInstrument instrument, Money amount, String reference) {
        String token = instrument.token().toLowerCase();
        if (token.endsWith("0000") || token.contains("fail")) {
            return GatewayResponse.declined("Declined by issuer");
        }
        return GatewayResponse.ok(Ids.next("txn"));
    }

    @Override
    public GatewayResponse refund(String gatewayReference, Money amount) {
        return GatewayResponse.ok(Ids.next("rfd"));
    }
}
