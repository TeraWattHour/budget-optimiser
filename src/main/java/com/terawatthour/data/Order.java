package com.terawatthour.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Order {
    private final String id;
    private final BigDecimal value;
    private final List<String> promotions;

    @JsonCreator
    public Order(@JsonProperty("id") String id,
                 @JsonProperty("value") String value,
                 @JsonProperty("promotions") List<String> promotions) {
        this.id = id;
        this.value = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
        this.promotions = promotions == null ? List.of() : promotions;
    }

    public String getId() { return id; }
    public BigDecimal getValue() { return value; }
    public List<String> getPromotions() { return promotions; }
}
