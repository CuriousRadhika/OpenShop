package com.openshop.paymentservice.controller;

import com.openshop.paymentservice.dto.PaymentRequest;
import com.openshop.paymentservice.dto.PaymentWebhookDTO;
import com.openshop.paymentservice.model.Payment;
import com.openshop.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentRequest> initiatePayment(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, @Valid @RequestBody PaymentRequest paymentRequest) {
        // Generate idempotency key if not provided
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            idempotencyKey = paymentRequest.getOrderId().toString() + "-payment";
        }
        log.info("Payment initiation request - orderId: {}, userId: {}, amount: {}, idempotencyKey: {}",
                paymentRequest.getOrderId(), paymentRequest.getUserId(), paymentRequest.getAmount(), idempotencyKey);
        Payment payment = paymentService.initiatePayment(paymentRequest, idempotencyKey);
        log.info("Payment initiated successfully - orderId: {}, transactionId: {}, status: {}", 
                payment.getOrderId(), payment.getTransactionId(), payment.getStatus());

        paymentRequest.setStatus("PENDING");
        return ResponseEntity.ok(paymentRequest);
    }


    @GetMapping("/{orderId}")
    public Optional<Payment> getPaymentStatus(@PathVariable UUID orderId) {
        log.debug("Payment status check - orderId: {}", orderId);
        Optional<Payment> payment = paymentService.getPaymentStatus(orderId);
        if (payment.isPresent()) {
            log.debug("Payment found - orderId: {}, status: {}", orderId, payment.get().getStatus());
        } else {
            log.debug("Payment not found - orderId: {}", orderId);
        }
        return payment;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handlePaymentWebhook(@RequestBody PaymentWebhookDTO webhookDTO) {
        // Log for debugging
        System.out.println("Received webhook: " + webhookDTO);

        // Process payment status
        paymentService.processPaymentWebhook(webhookDTO);

        // Return 200 OK to gateway
        return ResponseEntity.ok("Webhook received");
    }


}
