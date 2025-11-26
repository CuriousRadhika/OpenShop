package com.openshop.orderservice.client;

import com.openshop.orderservice.dto.PaymentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "payment-service", url = "${payment.service.url:http://payment-service:8084}")
public interface PaymentClient {
    @PostMapping("/api/payments")
    PaymentRequest initiatePayment(@RequestHeader(value = "Idempotency-Key") String idempotencyKey, PaymentRequest paymentRequest);
}
