package com.terawatthour;
import com.terawatthour.data.Order;
import com.terawatthour.data.PaymentMethod;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class TestOptimizer {
    @org.junit.Test
    public void testFullyPaid() {
        List<PaymentMethod> paymentMethods = generatePaymentMethods(100);
        List<Order> orders = generateOrders(10000, paymentMethods);

        PaymentOptimizer.optimizePayments(orders, paymentMethods);
    }

    @org.junit.Test
    public void testExactBudget() {
        List<PaymentMethod> paymentMethods = List.of(new PaymentMethod(
            "PUNKTY",
            new BigInteger("40"),
            new BigDecimal("3000")
        ));
        List<Order> orders = List.of(new Order("ORDER-1", "5000.00", List.of()));

        PaymentOptimizer.optimizePayments(orders, paymentMethods);
    }

    private List<Order> generateOrders(int count, List<PaymentMethod> paymentMethods) {
        BigDecimal budget = paymentMethods.stream().map(PaymentMethod::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        Order[] orders = new Order[count];
        int i = 0;
        for (; i < count && budget.compareTo(BigDecimal.ZERO) > 0; i++) {
            int pickMethods = Math.min(10, (int) (Math.random() * paymentMethods.size()));
            String[] pickedMethods = new String[pickMethods];
            for (int j = 0; j < pickMethods; j++) {
                pickedMethods[j] = paymentMethods.get((int) (Math.random() * paymentMethods.size())).getId();
            }

            BigDecimal cost = new BigDecimal(String.format("%.2f", Math.random() * 100f)).min(budget);
            orders[i] = new Order(String.format("ORDER-%d", i), cost.toString(), List.of(pickedMethods));
            budget = budget.subtract(cost);
        }
        return Arrays.asList(Arrays.copyOfRange(orders, 0, i));
    }

    private List<PaymentMethod> generatePaymentMethods(int count) {
        PaymentMethod[] methods = new PaymentMethod[count+1];
        methods[0] = new PaymentMethod("PUNKTY", new BigInteger(String.format("%d", (int) (Math.random() * 40))), new BigDecimal(String.format("%.2f", Math.random() * 5000f)));
        for (int i = 1; i < count+1; i++) {
            methods[i] = new PaymentMethod(String.format("METHOD-%d", i), new BigInteger(String.format("%d", (int) (Math.random() * 40))), new BigDecimal(String.format("%.2f", Math.random() * 10000f)));
        }
        return Arrays.asList(methods);
    }
}
