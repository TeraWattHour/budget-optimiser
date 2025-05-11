package com.terawatthour;

import java.math.BigDecimal;
import com.terawatthour.data.*;

import java.util.*;

public class PaymentOptimizer {
    static boolean DEBUGGING = false;

    public static HashMap<String, BigDecimal> optimizePayments(List<Order> orders, List<PaymentMethod> methods) throws Exception {
        List<PaymentMethod> candidateMethods = sortMethods(methods.stream().filter(method -> !method.isPoints()).toList());
        ArrayList<Order> left = new ArrayList<>(orders.stream().sorted((a, b) -> b.getValue().compareTo(a.getValue())).toList());

        PaymentMethod pointsMethod = methods.stream().filter(PaymentMethod::isPoints).findFirst().orElse(null);

        if (pointsMethod != null) {
            // 1. Try paying using points in full.
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
        }

        // 2. Pair the best methods with the best orders - use only full payments as to get the discount. Excludes the points method.
        // This is the only place where we care about the associated discount.
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


        if (pointsMethod != null) {

            // 3. Try paying using points for [0.1, 0.9] of the order value as not to waste the potential 10% discount.
            left.removeIf(order -> {
                BigDecimal orderValue = order.getValue();
                BigDecimal tenPercent = orderValue.multiply(new BigDecimal("0.1"));
                BigDecimal pointsUsed = orderValue.multiply(new BigDecimal("0.9")).min(pointsMethod.getBalance());
                BigDecimal discount = pointsUsed.compareTo(tenPercent) >= 0 ? tenPercent : BigDecimal.ZERO;

                // At this point, we want to maximise the discount, so we want to use at least 10% of the order value.
                if (pointsUsed.compareTo(tenPercent) < 0) {
                    return false;
                }

                orderValue = orderValue.subtract(pointsUsed).subtract(discount);

                // The payment methods are reversed to use the leftovers.
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

        // 4. Do whatever possible, don't care about discounts.
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

        if (!left.isEmpty()) {
            throw new Exception("Not all orders were paid for");
        }

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