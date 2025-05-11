package com.terawatthour;

import java.math.BigDecimal;
import com.terawatthour.data.*;

import java.util.*;

public class PaymentOptimizer {
    static boolean DEBUGGING = false;

    public static HashMap<String, BigDecimal> optimizePayments(List<Order> orders, List<PaymentMethod> methods) {
        List<PaymentMethod> candidateMethods = sortMethods(methods.stream().filter(method -> !method.isPoints()).toList());
        ArrayList<Order> left = new ArrayList<>(orders.stream().sorted((a, b) -> b.getValue().compareTo(a.getValue())).toList());

        // First, pair the best methods with the best orders - use only full payments as to get the discount. Excludes points.
        for (PaymentMethod method : candidateMethods) {
            Iterator<Order> candidates = left.stream().sorted((a, b) -> {
                BigDecimal aDiscount = a.getPromotions().contains(method.getId()) ? a.getValue().multiply(method.getDiscount()) : BigDecimal.ZERO;
                BigDecimal bDiscount = b.getPromotions().contains(method.getId()) ? b.getValue().multiply(method.getDiscount()) : BigDecimal.ZERO;
                return bDiscount.compareTo(aDiscount);
            }).iterator();

            while (candidates.hasNext() && method.hasMoneyFor(BigDecimal.ZERO)) {
                Order order = candidates.next();

                if (!order.getPromotions().contains(method.getId())) continue;

                BigDecimal orderValue = order.getValue();
                BigDecimal discount = order.getPromotions().contains(method.getId()) ? orderValue.multiply(method.getDiscount()) : BigDecimal.ZERO;
                BigDecimal paid = orderValue.subtract(discount);
                if (!method.hasMoneyFor(paid)) continue;

                method.spend(paid);
                left.remove(order);

                debug("Paid for order %s with method %s: %.2f (discount: %.2f)\n", order.getId(), method.getId(), paid, discount);
            }
        }

        PaymentMethod pointsMethod = methods.stream().filter(PaymentMethod::isPoints).findFirst().orElse(null);
        if (pointsMethod != null) {

            // Try paying using points in full
            int i = 0;
            for (; i < left.size(); i++) {
                Order order = left.get(i);
                BigDecimal orderValue = order.getValue();
                BigDecimal discount = orderValue.multiply(pointsMethod.getDiscount());
                BigDecimal paid = orderValue.subtract(discount);
                if (!pointsMethod.hasMoneyFor(paid)) {
                    left = new ArrayList<>(left.subList(i, left.size()));
                    break;
                }
                pointsMethod.spend(paid);
                debug("Paid for order %s with points: %.2f (discount: %.2f)\n", order.getId(), paid, discount);
                if (i == left.size() - 1) {
                    left = new ArrayList<>();
                    break;
                }
            }

            // Try paying using points for [0.1, 0.9] of the order value
            left.removeIf(order -> {
                BigDecimal orderValue = order.getValue();
                BigDecimal tenPercent = orderValue.multiply(new BigDecimal("0.1"));
                BigDecimal pointsUsed = orderValue.multiply(new BigDecimal("0.9")).min(pointsMethod.getBalance());
                BigDecimal discount = pointsUsed.compareTo(tenPercent) >= 0 ? tenPercent : BigDecimal.ZERO;

                if (pointsUsed.compareTo(tenPercent) < 0) {
                    return false;
                }

                orderValue = orderValue.subtract(pointsUsed).subtract(discount);

                for (PaymentMethod method : sortMethods(candidateMethods).reversed()) {
                    if (method.hasMoneyFor(orderValue)) {
                        pointsMethod.spend(pointsUsed);
                        method.spend(orderValue);
                        debug("Paid for order %s with partial points and method %s: %.2f (discount: %.2f)\n", order.getId(), method.getId(), orderValue, discount);
                        return true;
                    }
                }

                return false;
            });
        }

        // Do whatever possible, not caring about discounts
        left.removeIf(order -> {
            BigDecimal orderValue = order.getValue();
            BigDecimal points = pointsMethod != null ? pointsMethod.getBalance() : BigDecimal.ZERO;
            BigDecimal remaining = orderValue.subtract(points);
            if (pointsMethod != null) pointsMethod.spend(points);

            for (PaymentMethod method : sortMethods(candidateMethods).reversed()) {
                if (method.hasMoneyFor(remaining)) {
                    method.spend(remaining);
                    debug("Paid for order %s with method %s: %.2f (discount: %.2f)\n", order.getId(), method.getId(), remaining, BigDecimal.ZERO);
                    return true;
                }
            }

            return false;
        });

        assert left.isEmpty() : "Not all orders were paid for";

        HashMap<String, BigDecimal> methodUsage = new HashMap<>();
        for (PaymentMethod method : methods) {
            BigDecimal spent = method.getLimit().subtract(method.getBalance());
            methodUsage.put(method.getId(), spent);
        }

        return methodUsage;
    }

    private static void debug(String format, Object... args) {
        if (DEBUGGING) {
            System.out.printf(format, args);
        }
    }

    private static List<PaymentMethod> sortMethods(List<PaymentMethod> methods) {
        return methods.stream().sorted((a, b) -> b.getDiscount().compareTo(a.getDiscount())).toList();
    }
}