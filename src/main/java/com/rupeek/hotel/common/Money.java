package com.rupeek.hotel.common;

import com.rupeek.hotel.common.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Amount in INR, always held at 2 decimal places. Never negative. */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        if (amount == null) {
            throw new ValidationException("Amount is required");
        }
        if (amount.signum() < 0) {
            throw new ValidationException("Amount cannot be negative: " + amount);
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    public static Money of(long value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money times(long factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)));
    }

    public Money percent(int pct) {
        return new Money(amount.multiply(BigDecimal.valueOf(pct)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    public Money dividedBy(long divisor) {
        return new Money(amount.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }
}
