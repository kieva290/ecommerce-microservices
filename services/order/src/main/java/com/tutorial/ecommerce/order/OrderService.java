package com.tutorial.ecommerce.order;

import com.tutorial.ecommerce.customer.CustomerClient;
import com.tutorial.ecommerce.exception.BusinessException;
import com.tutorial.ecommerce.kafka.OrderConfirmation;
import com.tutorial.ecommerce.kafka.OrderProducer;
import com.tutorial.ecommerce.orderline.OrderLineRequest;
import com.tutorial.ecommerce.orderline.OrderLineService;
import com.tutorial.ecommerce.payment.PaymentClient;
import com.tutorial.ecommerce.payment.PaymentRequest;
import com.tutorial.ecommerce.product.ProductClient;
import com.tutorial.ecommerce.product.PurchaseRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final OrderLineService orderLineService;
    private final OrderProducer orderProducer;
    private final PaymentClient paymentClient;

    @Transactional
    public Integer createOrder(@Valid OrderRequest request) {

        // check the customer --> use OpenFeign, send request to customer-service
        var customer = this.customerClient.findCustomerById(request.customerId())
                .orElseThrow(() -> new BusinessException("Cannot create order:: No customer exists with the provided ID"));

        // purchase the product --> send request to --> product-service
        var purchasedProducts = productClient.purchaseProducts(request.products());

        // persist the order
        var order = this.repository.save(mapper.toOrder(request));

        // persist the order lines
        for (PurchaseRequest purchaseRequest : request.products()) {
            orderLineService.saveOrderLine(
                    new OrderLineRequest(
                            null,
                            order.getId(),
                            purchaseRequest.productId(),
                            purchaseRequest.quantity()
                    )
            );
        }

        // start payment process -- uses payment-service
        var paymentRequest = new PaymentRequest(
                request.amount(),
                request.paymentMethod(),
                order.getId(),
                order.getReference(),
                customer
        );
        paymentClient.requestOrderPayment(paymentRequest);

        // send the order confirmation --> notification-service (send message to Kafka broker)
        orderProducer.sendOrderConfirmation(
                new OrderConfirmation(
                        request.reference(),
                        request.amount(),
                        request.paymentMethod(),
                        customer,
                        purchasedProducts
                )
        );

        return order.getId();
    }

    public List<OrderResponse> findAll() {
        return this.repository.findAll()
                .stream()
                .map(this.mapper::fromOrder)
                .collect(Collectors.toList());
    }


    public OrderResponse findById(Integer orderId) {
        return this.repository.findById(orderId)
                .map(this.mapper::fromOrder)
                .orElseThrow(() ->new EntityNotFoundException(String.format("No order found with the provided ID: %d", orderId)));
    }
}
