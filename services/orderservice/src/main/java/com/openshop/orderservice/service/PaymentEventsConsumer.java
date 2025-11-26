package com.openshop.orderservice.service;

import com.openshop.events.constants.KafkaTopics;
import com.openshop.events.payment.PaymentResponseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentEventsConsumer {

    private final OrderSagaService orderSagaService;

    public PaymentEventsConsumer(OrderSagaService orderSagaService) {
        this.orderSagaService = orderSagaService;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_ORDER_RESPONSE, groupId = "order-service-group")
    public void consumePaymentEvent(PaymentResponseEvent event) {
        log.info("Received payment event for orderId {} with status {}", event.getOrderId(), event.getStatus());

        if ("SUCCESS".equals(event.getStatus())) {
            // Continue saga: initiate inventory reservation
            orderSagaService.processPaymentSuccess(event.getOrderId(), event.getTransactionId());
        } else if ("FAILED".equals(event.getStatus())) {
            // Compensating transaction: cancel order
            orderSagaService.processPaymentFailure(event.getOrderId(), event.getTransactionId());
        } else if ("REFUNDED".equals(event.getStatus())) {
            // Handle successful refund
            log.info("Payment refunded successfully for orderId: {}", event.getOrderId());
            orderSagaService.processPaymentRefundSuccess(event.getOrderId(), event.getTransactionId());
        } else if ("REFUND_FAILED".equals(event.getStatus())) {
            // Handle failed refund
            log.error("Payment refund failed for orderId: {}. Reason: {}", event.getOrderId(), event.getFailureReason());
            orderSagaService.processPaymentRefundFailure(event.getOrderId(), event.getFailureReason());
        } else {
            log.warn("Unknown payment status {} for orderId {}", event.getStatus(), event.getOrderId());
        }
    }
}
