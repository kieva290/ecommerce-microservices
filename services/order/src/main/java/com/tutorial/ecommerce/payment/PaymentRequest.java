package com.tutorial.ecommerce.payment;

import com.tutorial.ecommerce.customer.CustomerResponse;
import com.tutorial.ecommerce.order.PaymentMethod;

import java.math.BigDecimal;

public record PaymentRequest(
        BigDecimal amount,
        PaymentMethod paymentMethod,
        Integer orderId,
        String orderReference,
        CustomerResponse customer
) {
}
