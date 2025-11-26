package com.openshop.paymentservice.service;

import com.openshop.events.constants.KafkaTopics;
import com.openshop.events.payment.PaymentRefundRequestEvent;
import com.openshop.events.payment.PaymentResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRefundConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PaymentService paymentService;

    @KafkaListener(topics = KafkaTopics.PAYMENT_REFUND_REQUEST, groupId = "payment-service-group")
    public void consumeRefundRequest(PaymentRefundRequestEvent event) {
        log.info("Received payment refund request for orderId: {}", event.getOrderId());

        try {
            boolean paymentRefunded = paymentService.refundPayment(
                event.getOrderId(),
                event.getUserId(),
                event.getAmount()
            );

            // Publish response event
            PaymentResponseEvent response = PaymentResponseEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .transactionId(event.getTransactionId())
                    .status(paymentRefunded ? "REFUNDED" : "REFUND_FAILED")
                    .amount(event.getAmount())
                    .paymentMethod("CARD")
                    .failureReason(paymentRefunded ? null : "Technical failure during refund")
                    .correlationId(event.getCorrelationId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(KafkaTopics.PAYMENT_ORDER_RESPONSE, event.getOrderId().toString(), response);
            log.info("Published payment refund response for orderId: {} with status: {}", event.getOrderId(), response.getStatus());

        } catch (Exception e) {
            log.error("Error processing refund for orderId: {}", event.getOrderId(), e);

            // Publish failure response
            PaymentResponseEvent response = PaymentResponseEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .transactionId(event.getTransactionId())
                    .status("REFUND_FAILED")
                    .amount(event.getAmount())
                    .paymentMethod("CARD")
                    .failureReason("Refund processing error: " + e.getMessage())
                    .correlationId(event.getCorrelationId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(KafkaTopics.PAYMENT_ORDER_RESPONSE, event.getOrderId().toString(), response);
        }
    }
}
