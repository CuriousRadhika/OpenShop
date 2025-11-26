package com.openshop.orderservice.service;

import com.openshop.events.constants.KafkaTopics;
import com.openshop.events.inventory.InventoryReserveResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventsConsumer {

    private final OrderSagaService orderSagaService;

    @KafkaListener(topics = KafkaTopics.INVENTORY_ORDER_RESERVE_RESPONSE, groupId = "order-service-group")
    public void consumeInventoryEvent(InventoryReserveResponseEvent event) {
        log.info("Received inventory event for orderId {} with status {}", event.getOrderId(), event.getStatus());

        if ("SUCCESS".equals(event.getStatus())) {
            // Continue saga: initiate shipping
            orderSagaService.processInventoryReserveSuccess(event.getOrderId());
        } else if ("FAILED".equals(event.getStatus())) {
            // Compensating transaction: refund payment and cancel order
            orderSagaService.processInventoryReserveFailure(event.getOrderId(), event.getFailureReason());
        } else {
            log.warn("Unknown inventory status {} for orderId {}", event.getStatus(), event.getOrderId());
        }
    }
}
