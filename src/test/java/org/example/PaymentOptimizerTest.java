package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;

class PaymentOptimizerTest {

    private Map<String, PaymentMethod> createTestMethods() {
        Map<String, PaymentMethod> methods = new HashMap<>();
        methods.put("PUNKTY", new PaymentMethod("PUNKTY", "15", "100.00"));
        methods.put("mZysk", new PaymentMethod("mZysk", "10", "180.00"));
        methods.put("BosBankrut", new PaymentMethod("BosBankrut", "5", "200.00"));
        return methods;
    }

    private List<Order> createTestOrders() {
        return Arrays.asList(
                new Order("ORDER1", "100.00", Collections.singletonList("mZysk")),
                new Order("ORDER2", "200.00", Collections.singletonList("BosBankrut")),
                new Order("ORDER3", "150.00", Arrays.asList("mZysk", "BosBankrut")),
                new Order("ORDER4", "50.00", null)
        );
    }

    // Model class tests
    @Test
    void testOrderCreation() {
        Order order = new Order("TEST", "123.45", Arrays.asList("PROMO1", "PROMO2"));
        assertEquals("TEST", order.getId());
        assertEquals(new BigDecimal("123.45"), order.getValue());
        assertEquals(2, order.getPromotions().size());
    }

    @Test
    void testPaymentMethodDeduction() {
        PaymentMethod pm = new PaymentMethod("TEST", "10", "100.00");
        pm.deduct(new BigDecimal("50.00"));
        assertEquals(new BigDecimal("50.00"), pm.getRemaining());

        assertThrows(IllegalArgumentException.class,
                () -> pm.deduct(new BigDecimal("60.00")));
    }

    // Core algorithm tests
    @Test
    void testOptimizationLogic() {
        PaymentOptimizer optimizer = new PaymentOptimizer();
        Map<String, BigDecimal> result = optimizer.optimizePayments(
                createTestOrders(),
                createTestMethods()
        );

        // Verify totals match expected values
        assertAll(
                () -> assertEquals(new BigDecimal("90.00"), result.get("PUNKTY")),
                () -> assertEquals(new BigDecimal("175.00"), result.get("mZysk")),
                () -> assertEquals(new BigDecimal("190.00"), result.get("BosBankrut"))
        );
    }

    @Test
    void testPartialPointsPayment() {
        List<Order> orders = Collections.singletonList(
                new Order("TEST_ORDER", "50.00", null) // No promotions
        );

        Map<String, PaymentMethod> methods = createTestMethods();
        PaymentOptimizer optimizer = new PaymentOptimizer();

        Map<String, BigDecimal> result = optimizer.optimizePayments(orders, methods);

        assertEquals(new BigDecimal("42.50"), result.get("PUNKTY"));
    }

}