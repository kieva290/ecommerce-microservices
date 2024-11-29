package com.tutorial.ecommerce.kafka;

import com.tutorial.ecommerce.customer.CustomerResponse;
import com.tutorial.ecommerce.order.PaymentMethod;
import com.tutorial.ecommerce.product.PurchaseResponse;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmation(
        String orderReference,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        CustomerResponse customer,
        List<PurchaseResponse> products
) {
}
