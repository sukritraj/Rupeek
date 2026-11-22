package com.rupeek.hotel.payment;

import java.util.Map;

/**
 * A way of paying. It owns the validation of its own details and turns them into an
 * instrument the gateway understands. Add a method = add one @Component implementing this.
 */
public interface PaymentMethod {

    /** Identifier used in API requests, e.g. CARD, UPI, WALLET. */
    String type();

    /** Validates the method specific details and builds the instrument to charge. */
    PaymentInstrument prepare(Map<String, String> details);
}
