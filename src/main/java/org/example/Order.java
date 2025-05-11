package org.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

class Order {
    private final String id;
    private final BigDecimal value;
    private final List<String> promotions;

    @JsonCreator
    public Order(
            @JsonProperty("id") String id,
            @JsonProperty("value") String value,
            @JsonProperty("promotions") List<String> promotions) {
        this.id = id;
        this.value = new BigDecimal(value);
        this.promotions = promotions != null ? promotions : Collections.emptyList();
    }

    public String getId() {
        return id;
    }

    public BigDecimal getValue() {
        return value;
    }

    public List<String> getPromotions() {
        return promotions;
    }
}
