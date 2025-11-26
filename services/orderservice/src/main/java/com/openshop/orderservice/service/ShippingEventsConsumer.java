package com.openshop.orderservice.service;

import com.openshop.events.constants.KafkaTopics;
import com.openshop.events.shipping.ShippingResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingEventsConsumer {

    private final OrderSagaService orderSagaService;

    @KafkaListener(topics = KafkaTopics.SHIPPING_ORDER_RESPONSE, groupId = "order-service-group")
    public void consumeShippingEvent(ShippingResponseEvent event) {
        log.info("Received shipping event for orderId {} with status {}", event.getOrderId(), event.getStatus());

        if ("SUCCESS".equals(event.getStatus())) {
            // Complete saga: order confirmed
            orderSagaService.processShippingSuccess(
                event.getOrderId(),
                event.getShipmentId().toString(), 
                event.getTrackingNumber()
            );
        } else if ("FAILED".equals(event.getStatus())) {
            // Compensating transaction: restore inventory, refund payment, cancel order
            orderSagaService.processShippingFailure(event.getOrderId(), event.getFailureReason());
        } else {
            log.warn("Unknown shipping status {} for orderId {}", event.getStatus(), event.getOrderId());
        }
    }
}
