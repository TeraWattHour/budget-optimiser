package com.terawatthour;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terawatthour.data.Order;
import com.terawatthour.data.PaymentMethod;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("USAGE: optimizer <path to orders file> <path to payment methods file>");
            System.exit(1);
        }

        String ordersFilePath = args[0];
        String paymentMethodsFilePath = args[1];

        Order[] orders = readJSONFile(ordersFilePath, Order[].class);
        PaymentMethod[] paymentMethods = readJSONFile(paymentMethodsFilePath, PaymentMethod[].class);

        HashMap<String, BigDecimal> solution = PaymentOptimizer.optimizePayments(List.of(orders), List.of(paymentMethods));
        solution.forEach((k, v) -> System.out.printf("%s %.2f\n", k, v));
    }

    private static<T> T readJSONFile(String filePath, Class<T> genericType) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(new File(filePath), genericType);
    }
}

