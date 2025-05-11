package org.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

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

class PaymentOptimizer {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TEN = new BigDecimal("10");
    private static final BigDecimal PARTIAL_POINTS_DISCOUNT = new BigDecimal("10");
    private static final String POINTS_ID = "PUNKTY";
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private static class PaymentOption {
        private final String cardId;
        private final BigDecimal cardAmount;
        private final BigDecimal pointsAmount;
        private final BigDecimal discount;
        private final BigDecimal savings;

        public PaymentOption(String cardId, BigDecimal cardAmount, BigDecimal pointsAmount,
                             BigDecimal discount, BigDecimal savings) {
            this.cardId = cardId;
            this.cardAmount = cardAmount;
            this.pointsAmount = pointsAmount;
            this.discount = discount;
            this.savings = savings;
        }
    }

    public Map<String, BigDecimal> optimizePayments(List<Order> orders, Map<String, PaymentMethod> paymentMethods) {
        Map<String, PaymentMethod> methods = clonePaymentMethods(paymentMethods);
        Map<String, BigDecimal> allocations = new HashMap<>();
        methods.keySet().forEach(k -> allocations.put(k, BigDecimal.ZERO));

        orders.stream()
                .sorted(Comparator.comparingInt((Order o) -> -getMaxDiscount(o, methods))
                        .thenComparing(Order::getValue, Comparator.reverseOrder()))
                .forEach(order -> processOrder(order, methods, allocations));

        return allocations.entrySet().stream()
                .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private int getMaxDiscount(Order order, Map<String, PaymentMethod> methods) {
        return order.getPromotions().stream()
                .map(methods::get)
                .filter(Objects::nonNull)
                .mapToInt(PaymentMethod::getDiscount)
                .max()
                .orElse(0);
    }

    private void processOrder(Order order, Map<String, PaymentMethod> methods, Map<String, BigDecimal> allocations) {
        List<PaymentOption> options = new ArrayList<>();
        BigDecimal orderValue = order.getValue();
        PaymentMethod points = methods.get(POINTS_ID);

        // Full points payment
        if (points != null && points.getRemaining().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = orderValue.multiply(new BigDecimal(points.getDiscount())).divide(HUNDRED, SCALE, ROUNDING_MODE);
            BigDecimal finalAmount = orderValue.subtract(discount);
            if (points.getRemaining().compareTo(finalAmount) >= 0) {
                options.add(new PaymentOption(null, BigDecimal.ZERO, finalAmount, discount, discount));
            }
        }

        // Full card payment with promotions
        for (String promo : order.getPromotions()) {
            PaymentMethod method = methods.get(promo);
            if (method != null && !method.isLoyaltyPoints() && method.getRemaining().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discount = orderValue.multiply(new BigDecimal(method.getDiscount())).divide(HUNDRED, SCALE, ROUNDING_MODE);
                BigDecimal finalAmount = orderValue.subtract(discount);
                if (method.getRemaining().compareTo(finalAmount) >= 0) {
                    options.add(new PaymentOption(method.getId(), finalAmount, BigDecimal.ZERO, discount, discount));
                }
            }
        }

        // Partial points (min 10%)
        if (points != null && points.getRemaining().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal minPoints = orderValue.multiply(TEN).divide(HUNDRED, SCALE, ROUNDING_MODE);
            if (points.getRemaining().compareTo(minPoints) >= 0) {
                BigDecimal discount = orderValue.multiply(PARTIAL_POINTS_DISCOUNT).divide(HUNDRED, SCALE, ROUNDING_MODE);
                BigDecimal remainingPayment = orderValue.subtract(discount).subtract(minPoints);
                for (PaymentMethod method : methods.values()) {
                    if (!method.isLoyaltyPoints() && method.getRemaining().compareTo(remainingPayment) >= 0) {
                        options.add(new PaymentOption(method.getId(), remainingPayment, minPoints, discount, discount));
                    }
                }
            }
        }

        // Fallback: any available payment
        for (PaymentMethod method : methods.values()) {
            if (!method.isLoyaltyPoints() && method.getRemaining().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal maxPay = method.getRemaining().min(orderValue);
                options.add(new PaymentOption(method.getId(), maxPay, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
            }
        }

        // Select best option
        options.sort((a, b) -> b.savings.compareTo(a.savings) != 0 ? b.savings.compareTo(a.savings) :
                a.pointsAmount.compareTo(b.pointsAmount));

        for (PaymentOption opt : options) {
            if (tryApplyOption(opt, methods, allocations)) {
                return;
            }
        }
        throw new IllegalStateException("Cannot process order: " + order.getId());
    }

    private boolean tryApplyOption(PaymentOption opt, Map<String, PaymentMethod> methods, Map<String, BigDecimal> allocations) {
        try {
            if (opt.pointsAmount.compareTo(BigDecimal.ZERO) > 0) {
                PaymentMethod p = methods.get(POINTS_ID);
                p.deduct(opt.pointsAmount);
                allocations.put(POINTS_ID, allocations.get(POINTS_ID).add(opt.pointsAmount));
            }
            if (opt.cardId != null && opt.cardAmount.compareTo(BigDecimal.ZERO) > 0) {
                PaymentMethod m = methods.get(opt.cardId);
                m.deduct(opt.cardAmount);
                allocations.put(opt.cardId, allocations.get(opt.cardId).add(opt.cardAmount));
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Map<String, PaymentMethod> clonePaymentMethods(Map<String, PaymentMethod> original) {
        return original.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new PaymentMethod(e.getValue().getId(),
                                String.valueOf(e.getValue().getDiscount()),
                                e.getValue().getLimit().toString()))
                );
    }
}

public class Main {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java -jar app.jar <orders.json> <paymentmethods.json>");
            System.exit(1);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Order> orders = mapper.readValue(new File(args[0]), new TypeReference<List<Order>>() {});
            List<PaymentMethod> methods = mapper.readValue(new File(args[1]), new TypeReference<List<PaymentMethod>>() {});

            Map<String, PaymentMethod> methodMap = methods.stream()
                    .collect(Collectors.toMap(PaymentMethod::getId, pm -> pm));

            Map<String, BigDecimal> result = new PaymentOptimizer().optimizePayments(orders, methodMap);

            result.forEach((k, v) -> System.out.printf("%s %.2f%n", k, v.setScale(2, RoundingMode.HALF_UP)));

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}