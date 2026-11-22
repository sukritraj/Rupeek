package com.rupeek.hotel.payment;

import com.rupeek.hotel.common.Money;

/** Our seam to the third-party processor. The only implementation here is a mock. */
public interface PaymentGateway {

    GatewayResponse charge(PaymentInstrument instrument, Money amount, String reference);

    GatewayResponse refund(String gatewayReference, Money amount);

    record GatewayResponse(boolean success, String reference, String message) {

        public static GatewayResponse ok(String reference) {
            return new GatewayResponse(true, reference, "OK");
        }

        public static GatewayResponse declined(String message) {
            return new GatewayResponse(false, null, message);
        }
    }
}
