package com.openshop.paymentservice.service;


import com.openshop.paymentservice.dto.PaymentRequest;
import com.openshop.paymentservice.dto.PaymentWebhookDTO;
import com.openshop.paymentservice.model.Payment;
import com.openshop.paymentservice.repository.PaymentRepository;
import com.openshop.events.payment.PaymentResponseEvent;
import com.openshop.events.constants.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 1️⃣ Start payment with idempotency support
    @Transactional
    public Payment initiatePayment(PaymentRequest paymentRequest, String idempotencyKey) {
        
        // Check if payment with this idempotency key already exists
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Duplicate payment request detected for idempotency key: {}, returning existing payment", idempotencyKey);
            return existing.get();
        }

        Payment payment = Payment.builder()
                .orderId(paymentRequest.getOrderId())
                .userId(paymentRequest.getUserId())
                .amount(paymentRequest.getAmount())
                .transactionId(UUID.randomUUID().toString())
                .status("INITIATED")
                .idempotencyKey(idempotencyKey)
                .timestamp(LocalDateTime.now())
                .build();

        //  Logic For Payment Gateway Integration would go here (omitted for brevity)

        log.info("Initiating new payment for order: {} with idempotency key: {}", paymentRequest.getOrderId(), idempotencyKey);
        return paymentRepository.save(payment);
    }



    public Optional<Payment> getPaymentStatus(UUID orderId) {
        log.debug("Fetching payment status for orderId: {}", orderId);
        return paymentRepository.findByOrderId(orderId);
    }

    /**
     * Process payment from Kafka event
     */
    public boolean processPayment(UUID orderId, Long userId, Double amount) {
        log.info("Processing payment for orderId: {}, amount: {}", orderId, amount);

        // Simulate payment processing (integrate with actual payment gateway)
        // For now, assume payment succeeds
        Payment payment = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .transactionId("txn-" + orderId)
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);
        log.info("Payment processed successfully for orderId: {}", orderId);
        return true; // Return false to simulate payment failure
    }

    public void processPaymentWebhook(PaymentWebhookDTO webhookDTO) {
        if(Objects.equals(webhookDTO.getStatus(), "FAILED")){
            log.info("Processing failed payment webhook for orderId: {}", webhookDTO.getOrderId());

            PaymentResponseEvent paymentResponseEvent = PaymentResponseEvent.builder()
                    .orderId(webhookDTO.getOrderId())
                    .userId(null) // Webhook doesn't have userId, use placeholder
                    .transactionId(webhookDTO.getTransactionId())
                    .status("FAILED")
                    .amount(webhookDTO.getAmount())
                    .paymentMethod(webhookDTO.getPaymentMethod())
                    .failureReason("Payment gateway reported failure")
                    .correlationId(String.valueOf(webhookDTO.getOrderId()))
                    .timestamp(System.currentTimeMillis())
                    .build();


            if(paymentRepository.findByOrderId(webhookDTO.getOrderId()).isPresent()){
                log.info("Updating payment status to FAILED for orderId: {}", webhookDTO.getOrderId());
                Payment payment = paymentRepository.findByOrderId(webhookDTO.getOrderId()).get();
                payment.setStatus("FAILED");
                paymentRepository.save(payment);
            }

            

            kafkaTemplate.send(KafkaTopics.PAYMENT_ORDER_RESPONSE, String.valueOf(webhookDTO.getOrderId()), paymentResponseEvent);

        } else if (Objects.equals(webhookDTO.getStatus(), "SUCCESS")){
            log.info("Processing successful payment webhook for orderId: {}", webhookDTO.getOrderId());
            
            PaymentResponseEvent paymentResponseEvent = PaymentResponseEvent.builder()
                    .orderId(webhookDTO.getOrderId())
                    .userId(null) // Webhook doesn't have userId, use placeholder
                    .transactionId(webhookDTO.getTransactionId())
                    .status("SUCCESS")
                    .amount(webhookDTO.getAmount())
                    .paymentMethod(webhookDTO.getPaymentMethod())
                    .failureReason(null)
                    .correlationId(String.valueOf(webhookDTO.getOrderId()))
                    .timestamp(System.currentTimeMillis())
                    .build();

            if(paymentRepository.findByOrderId(webhookDTO.getOrderId()).isPresent()){
                log.info("Updating payment status to SUCCESS for orderId: {}", webhookDTO.getOrderId());
                Payment payment = paymentRepository.findByOrderId(webhookDTO.getOrderId()).get();
                payment.setStatus("SUCCESS");
                paymentRepository.save(payment);
            }

            kafkaTemplate.send(KafkaTopics.PAYMENT_ORDER_RESPONSE, String.valueOf(webhookDTO.getOrderId()), paymentResponseEvent);
        }
    }

    public boolean refundPayment(UUID orderId, Long userId, Double amount) {
        log.info("Refunding payment for orderId: {}, amount: {}", orderId, amount);

        Optional<Payment> paymentOptional = paymentRepository.findByOrderId(orderId);
        
        if (paymentOptional.isPresent()) {
            Payment payment = paymentOptional.get();
            
            // Check if payment exists and was previously successful
            if ("SUCCESS".equals(payment.getStatus())) {
                payment.setStatus("REFUNDED");
                paymentRepository.save(payment);
                log.info("Payment refunded successfully for orderId: {}", orderId);
                return true;
            } else {
                log.warn("Cannot refund payment for orderId: {}. Current status: {}", orderId, payment.getStatus());
                return false;
            }
        } else {
            log.error("Payment not found for orderId: {}", orderId);
            return false;
        }
    }
}
