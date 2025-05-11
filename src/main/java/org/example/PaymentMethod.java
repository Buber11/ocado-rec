package org.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

class PaymentMethod {
    private final String id;
    private final int discount;
    private final BigDecimal limit;
    private BigDecimal remaining;

    @JsonCreator
    public PaymentMethod(
            @JsonProperty("id") String id,
            @JsonProperty("discount") String discount,
            @JsonProperty("limit") String limit) {
        this.id = id;
        this.discount = Integer.parseInt(discount);
        this.limit = new BigDecimal(limit);
        this.remaining = new BigDecimal(limit);
    }

    public String getId() {
        return id;
    }

    public int getDiscount() {
        return discount;
    }

    public BigDecimal getLimit() {
        return limit;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }

    public void deduct(BigDecimal amount) {
        if (amount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException("Insufficient balance for " + id);
        }
        remaining = remaining.subtract(amount);
    }

    public boolean isLoyaltyPoints() {
        return "PUNKTY".equals(id);
    }
}
