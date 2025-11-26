package com.openshop.orderservice.client;

import com.openshop.orderservice.dto.PaymentRequest;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentClientFallback {

    /**
     * Fallback method for payment initiation when circuit breaker is open
     * or when payment service is unavailable
     */
    public PaymentRequest initiatePaymentFallback(String idempotencyKey, PaymentRequest paymentRequest, Throwable throwable) {
        log.error("Payment service unavailable for order: {}. Circuit breaker activated. Error: {}", 
                paymentRequest.getOrderId(), throwable.getMessage());
        
        // Return a response indicating payment service is unavailable
        // The calling service should handle this appropriately
        PaymentRequest fallbackResponse = PaymentRequest.builder()
                .orderId(paymentRequest.getOrderId())
                .userId(paymentRequest.getUserId())
                .amount(paymentRequest.getAmount())
                .status("PAYMENT_SERVICE_UNAVAILABLE")
                .build();
        
        // Log circuit breaker activation for monitoring
        if (throwable instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            log.warn("Circuit breaker is OPEN for payment service. Order: {}", paymentRequest.getOrderId());
        } else if (throwable instanceof FeignException) {
            FeignException feignException = (FeignException) throwable;
            log.error("Feign error calling payment service. Status: {}, Message: {}", 
                    feignException.status(), feignException.getMessage());
        }
        
        return fallbackResponse;
    }
}
