package com.rupeek.hotel.payment;

/**
 * What the gateway is asked to charge. {@code token} is the raw credential (card number, VPA,
 * wallet id) and stays inside the payment package; {@code label} is the safe form we store/show.
 */
public record PaymentInstrument(String methodType, String label, String token) {
}
