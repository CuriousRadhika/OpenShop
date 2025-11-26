package com.openshop.paymentservice.service;

import com.openshop.events.constants.KafkaTopics;
import com.openshop.events.order.OrderPaymentRequestEvent;

import com.openshop.events.payment.PaymentResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PaymentService paymentService;



    @KafkaListener(topics = KafkaTopics.ORDER_PAYMENT_REQUEST, groupId = "payment-service-group")
    public void consumePaymentRequest(OrderPaymentRequestEvent event) {
        log.info("Received payment request for orderId: {}", event.getOrderId());

        try {
            boolean paymentSuccess = paymentService.processPayment(
                event.getOrderId(),
                event.getUserId(),
                event.getAmount()
            );

            // Publish response event
            PaymentResponseEvent response = PaymentResponseEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .transactionId("txn-" + event.getOrderId())
                    .status(paymentSuccess ? "SUCCESS" : "FAILED")
                    .amount(event.getAmount())
                    .paymentMethod("CARD")
                    .failureReason(paymentSuccess ? null : "Payment processing failed")
                    .correlationId(event.getCorrelationId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(KafkaTopics.PAYMENT_ORDER_RESPONSE, event.getOrderId().toString(), response);
            log.info("Published payment response for orderId: {} with status: {}", event.getOrderId(), response.getStatus());

        } catch (Exception e) {
            log.error("Error processing payment for orderId: {}", event.getOrderId(), e);

            // Publish failure response
            PaymentResponseEvent response = PaymentResponseEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .transactionId("txn-" + event.getOrderId())
                    .status("FAILED")
                    .amount(event.getAmount())
                    .paymentMethod("CARD")
                    .failureReason("Payment processing error: " + e.getMessage())
                    .correlationId(event.getCorrelationId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(KafkaTopics.PAYMENT_ORDER_RESPONSE, event.getOrderId().toString(), response);
        }
    }

}
