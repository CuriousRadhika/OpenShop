package com.openshop.inventoryservice.service;

import com.openshop.events.constants.KafkaTopics;
import com.openshop.events.inventory.InventoryReserveResponseEvent;
import com.openshop.events.inventory.InventoryRestoreRequestEvent;
import com.openshop.events.order.OrderInventoryReserveRequestEvent;
import com.openshop.inventoryservice.model.InventoryReservation;
import com.openshop.inventoryservice.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryRequestConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final InventoryService inventoryService;
    private final InventoryReservationRepository reservationRepository;

    @Transactional
    @KafkaListener(topics = KafkaTopics.ORDER_INVENTORY_RESERVE_REQUEST, groupId = "inventory-service-group")
    public void consumeInventoryReserveRequest(OrderInventoryReserveRequestEvent event) {
        log.info("Received inventory reserve request for orderId: {}", event.getOrderId());

        // IDEMPOTENCY CHECK: Check if this request has already been processed
        Optional<InventoryReservation> existingReservation = reservationRepository.findByCorrelationId(event.getCorrelationId());
        if (existingReservation.isPresent()) {
            log.info("Duplicate inventory reserve request detected for correlationId: {}. Returning cached response.", event.getCorrelationId());
            
            // Return the previous response to maintain idempotency
            InventoryReserveResponseEvent cachedResponse = InventoryReserveResponseEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .status(existingReservation.get().getStatus())
                    .failureReason(existingReservation.get().getStatus().equals("FAILED") ? "Previously failed" : null)
                    .reservedItems(new ArrayList<>()) // Could deserialize from details if needed
                    .correlationId(event.getCorrelationId())
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            kafkaTemplate.send(KafkaTopics.INVENTORY_ORDER_RESERVE_RESPONSE, event.getOrderId().toString(), cachedResponse);
            return;
        }

        try {
            List<InventoryReserveResponseEvent.ReservedItem> reservedItems = new ArrayList<>();
            List<OrderInventoryReserveRequestEvent.InventoryItem> successfullyReservedItems = new ArrayList<>();
            boolean allReserved = true;
            String failureReason = null;

            // Try to reserve inventory for each item
            for (OrderInventoryReserveRequestEvent.InventoryItem item : event.getItems()) {
                try {
                    boolean reserved = inventoryService.reduceInventory(
                        item.getProductId(),
                        item.getQuantity()
                    );

                    reservedItems.add(InventoryReserveResponseEvent.ReservedItem.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .reserved(reserved)
                            .build());

                    if (!reserved) {
                        allReserved = false;
                        failureReason = "Insufficient inventory for product: " + item.getProductId();
                        
                        // ROLLBACK: Restore previously reserved items
                        log.warn("Inventory reservation failed for product: {}. Rolling back previously reserved items", item.getProductId());
                        for (OrderInventoryReserveRequestEvent.InventoryItem successfulItem : successfullyReservedItems) {
                            try {
                                inventoryService.increaseInventory(
                                    successfulItem.getProductId().toString(),
                                    successfulItem.getQuantity()
                                );
                                log.info("Rolled back inventory for product: {} quantity: {}", 
                                    successfulItem.getProductId(), successfulItem.getQuantity());
                            } catch (Exception rollbackEx) {
                                log.error("CRITICAL: Failed to rollback inventory for product: {}. Manual intervention required!", 
                                    successfulItem.getProductId(), rollbackEx);
                            }
                        }
                        break;
                    } else {
                        // Track successfully reserved items for potential rollback
                        successfullyReservedItems.add(item);
                    }
                } catch (Exception e) {
                    log.error("Error reserving inventory for product: {}", item.getProductId(), e);
                    allReserved = false;
                    failureReason = "Error reserving inventory: " + e.getMessage();
                    
                    reservedItems.add(InventoryReserveResponseEvent.ReservedItem.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .reserved(false)
                            .build());
                    
                    // ROLLBACK: Restore previously reserved items
                    log.warn("Exception during inventory reservation. Rolling back previously reserved items");
                    for (OrderInventoryReserveRequestEvent.InventoryItem successfulItem : successfullyReservedItems) {
                        try {
                            inventoryService.increaseInventory(
                                successfulItem.getProductId().toString(),
                                successfulItem.getQuantity()
                            );
                            log.info("Rolled back inventory for product: {} quantity: {}", 
                                successfulItem.getProductId(), successfulItem.getQuantity());
                        } catch (Exception rollbackEx) {
                            log.error("CRITICAL: Failed to rollback inventory for product: {}. Manual intervention required!", 
                                successfulItem.getProductId(), rollbackEx);
                        }
                    }
                    break;
                }
            }

            // Save reservation record for idempotency
            InventoryReservation reservation = InventoryReservation.builder()
                    .orderId(event.getOrderId())
                    .correlationId(event.getCorrelationId())
                    .status(allReserved ? "RESERVED" : "FAILED")
                    .details("Reserved " + successfullyReservedItems.size() + " items")
                    .build();
            reservationRepository.save(reservation);
            log.info("Saved inventory reservation record for correlationId: {}", event.getCorrelationId());

            // Publish response event
            InventoryReserveResponseEvent response = InventoryReserveResponseEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .status(allReserved ? "SUCCESS" : "FAILED")
                    .failureReason(failureReason)
                    .reservedItems(reservedItems)
                    .correlationId(event.getCorrelationId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(KafkaTopics.INVENTORY_ORDER_RESERVE_RESPONSE, event.getOrderId().toString(), response);
            log.info("Published inventory reserve response for orderId: {} with status: {}", event.getOrderId(), response.getStatus());

        } catch (Exception e) {
            log.error("Error processing inventory reserve request for orderId: {}", event.getOrderId(), e);
            
            // Publish failure response
            InventoryReserveResponseEvent response = InventoryReserveResponseEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .status("FAILED")
                    .failureReason("Inventory processing error: " + e.getMessage())
                    .reservedItems(new ArrayList<>())
                    .correlationId(event.getCorrelationId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(KafkaTopics.INVENTORY_ORDER_RESERVE_RESPONSE, event.getOrderId().toString(), response);
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDER_INVENTORY_RESTORE_REQUEST, groupId = "inventory-service-group")
    public void consumeInventoryRestoreRequest(InventoryRestoreRequestEvent event) {
        log.info("Received inventory restore request for orderId: {}", event.getOrderId());

        try {
            // Restore inventory for each item
            for (InventoryRestoreRequestEvent.RestoreItem item : event.getItems()) {
                inventoryService.increaseInventory(
                    item.getProductId().toString(),
                    item.getQuantity()
                );
                log.info("Restored inventory for product: {} quantity: {}", item.getProductId(), item.getQuantity());
            }

            log.info("Successfully restored inventory for order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("CRITICAL: Error restoring inventory for orderId: {} - manual intervention required", event.getOrderId(), e);
        }
    }
}
