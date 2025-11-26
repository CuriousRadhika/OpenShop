package com.openshop.orderservice.service;

import com.openshop.orderservice.client.CartClient;
import com.openshop.orderservice.client.PaymentClient;
import com.openshop.orderservice.dto.CartDTO;
import com.openshop.orderservice.dto.CreateOrderRequest;
import com.openshop.orderservice.dto.PaymentRequest;
import com.openshop.orderservice.model.Order;
import com.openshop.orderservice.model.OrderItem;
import com.openshop.orderservice.model.OrderStatus;
import com.openshop.orderservice.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderSagaService orderSagaService;
    private final CartClient cartClient;
    private final PaymentClient paymentClient;

    public Order createOrder(Long userId, String idempotencyKey, CreateOrderRequest deliveryParams) {
        CartDTO cartDTO = cartClient.getCart(userId);

        if (cartDTO.getItems() == null || cartDTO.getItems().isEmpty()) {
            log.warn("Checkout failed for userId: {} - Cart is empty", userId);
            return null;
        }

        List<OrderItem> orderItems = cartDTO.getItems().stream()
                .map(item -> OrderItem.builder()
                        .productId(UUID.fromString(item.getProductId()))
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        double totalPrice = orderItems.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();

        Order order = Order.builder()
                .userId(cartDTO.getUserId())
                .totalPrice(totalPrice)
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .items(orderItems)
                .idempotencyKey(idempotencyKey)
                .build();

        // Save order first to generate ID
        orderRepository.save(order);
        
        // Store delivery params in order for later use (not persisted, just in memory)
        // This will be used when inventory is reserved to send to shipping service
        orderSagaService.storeDeliveryParams(order.getId(), deliveryParams);

        cartClient.clearCart(userId);

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .status(OrderStatus.CREATED.name())
                .amount(totalPrice)
                .build();

        initiatePaymentWithCircuitBreaker(idempotencyKey, paymentRequest);

        return order;
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order cancelOrder(UUID orderId, Long userId, String reason) {
        orderSagaService.cancelOrder(orderId, userId, reason);
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    /**
     * Initiates payment with circuit breaker protection
     * Falls back to fallback method if payment service is unavailable
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "initiatePaymentFallback")
    private PaymentRequest initiatePaymentWithCircuitBreaker(String idempotencyKey, PaymentRequest paymentRequest) {
        log.info("Initiating payment for order: {} with circuit breaker", paymentRequest.getOrderId());
        return paymentClient.initiatePayment(idempotencyKey, paymentRequest);
    }


    public Order getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }
}
