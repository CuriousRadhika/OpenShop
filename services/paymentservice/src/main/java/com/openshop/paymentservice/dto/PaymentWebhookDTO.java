package com.openshop.paymentservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PaymentWebhookDTO {

    private UUID orderId;          // Internal Order ID
    private String status;           // SUCCESS / FAILURE
    private String transactionId;    // Payment transaction ID from gateway
    private Double amount;           // Paid amount
    private String currency;         // e.g., USD, INR
    private String paymentMethod;    // CARD, UPI, WALLET, etc.
    private String signature;        // Optional: to verify authenticity
    private Long timestamp;

}
