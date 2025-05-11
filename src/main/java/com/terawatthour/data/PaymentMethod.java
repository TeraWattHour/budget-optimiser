package com.terawatthour.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class PaymentMethod {
    private final String id;
    private final BigDecimal discount;
    private final BigDecimal limit;
    private BigDecimal balance;

    @JsonCreator
    public PaymentMethod(
            @JsonProperty("id") String id,
            @JsonProperty("discount") String discount,
            @JsonProperty("limit") String limit) {
        this.id = id;
        this.discount = new BigDecimal(discount).divide(new BigDecimal("100.0"), 2, RoundingMode.HALF_UP);
        this.limit = new BigDecimal(limit);
        this.balance = this.limit;
    }

    public PaymentMethod(String id, BigInteger discount, BigDecimal balance) {
        this.id = id;
        this.discount = new BigDecimal(discount).divide(new BigDecimal("100.0"), 2, RoundingMode.HALF_UP);
        this.limit = balance;
        this.balance = balance;
    }

    public String getId() { return id; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getLimit() { return limit; }

    public void spend(BigDecimal amount) {
        balance = balance.subtract(amount);
    }
    public boolean hasMoneyFor(BigDecimal amount) {
        return balance.subtract(amount).compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean isPoints() {
        return "PUNKTY".equals(id);
    }
}
