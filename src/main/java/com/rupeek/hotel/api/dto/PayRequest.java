package com.rupeek.hotel.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/** {@code details} depends on the method: CARD(cardNumber, expiry), UPI(vpa), WALLET(provider, walletId). */
public record PayRequest(@NotBlank String method, Map<String, String> details) {
}
