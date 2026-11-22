package com.rupeek.hotel.payment;

import com.rupeek.hotel.common.Ids;
import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.common.exception.InvalidStateTransitionException;

import java.time.Instant;

public class Payment {

    private final String id;
    private final String bookingId;
    private final String methodType;
    private final String instrumentLabel;
    private final Money amount;
    private final String idempotencyKey;
    private final String gatewayReference;
    private final String failureReason;
    private final Instant createdAt;

    private volatile PaymentStatus status;
    private volatile Money refundedAmount = Money.ZERO;

    private Payment(String bookingId, String methodType, String instrumentLabel, Money amount, String idempotencyKey,
                    PaymentStatus status, String gatewayReference, String failureReason, Instant now) {
        this.id = Ids.next("pay");
        this.bookingId = bookingId;
        this.methodType = methodType;
        this.instrumentLabel = instrumentLabel;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.gatewayReference = gatewayReference;
        this.failureReason = failureReason;
        this.createdAt = now;
    }

    public static Payment succeeded(String bookingId, PaymentInstrument instrument, Money amount,
                                    String idempotencyKey, String gatewayReference, Instant now) {
        return new Payment(bookingId, instrument.methodType(), instrument.label(), amount, idempotencyKey,
                PaymentStatus.SUCCESS, gatewayReference, null, now);
    }

    public static Payment failed(String bookingId, PaymentInstrument instrument, Money amount,
                                 String idempotencyKey, String reason, Instant now) {
        return new Payment(bookingId, instrument.methodType(), instrument.label(), amount, idempotencyKey,
                PaymentStatus.FAILED, null, reason, now);
    }

    public void recordRefund(Money refund) {
        if (status != PaymentStatus.SUCCESS && status != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new InvalidStateTransitionException("Payment " + id + " is " + status + " and cannot be refunded");
        }
        Money total = refundedAmount.plus(refund);
        if (total.isGreaterThan(amount)) {
            throw new InvalidStateTransitionException("Refund would exceed the amount paid on payment " + id);
        }
        this.refundedAmount = total;
        this.status = total.compareTo(amount) == 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
    }

    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCESS || status == PaymentStatus.PARTIALLY_REFUNDED
                || status == PaymentStatus.REFUNDED;
    }

    public String id() {
        return id;
    }

    public String bookingId() {
        return bookingId;
    }

    public String methodType() {
        return methodType;
    }

    public String instrumentLabel() {
        return instrumentLabel;
    }

    public Money amount() {
        return amount;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String gatewayReference() {
        return gatewayReference;
    }

    public String failureReason() {
        return failureReason;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public PaymentStatus status() {
        return status;
    }

    public Money refundedAmount() {
        return refundedAmount;
    }
}
