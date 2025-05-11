package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

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