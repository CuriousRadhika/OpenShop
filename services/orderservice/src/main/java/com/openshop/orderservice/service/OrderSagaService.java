package com.openshop.orderservice.service;

import com.openshop.events.constants.KafkaTopics;
import com.openshop.events.inventory.InventoryRestoreRequestEvent;
import com.openshop.events.order.OrderInventoryReserveRequestEvent;
import com.openshop.events.order.OrderNotificationRequestEvent;
import com.openshop.events.order.OrderPaymentRequestEvent;
import com.openshop.events.order.OrderShippingRequestEvent;
import com.openshop.events.payment.PaymentRefundRequestEvent;
import com.openshop.orderservice.dto.CreateOrderRequest;
import com.openshop.orderservice.model.Order;
import com.openshop.orderservice.model.OrderStatus;
import com.openshop.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSagaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;
    
    // In-memory storage for delivery parameters (not persisted in DB)
    private final Map<UUID, CreateOrderRequest> deliveryParamsCache = new ConcurrentHashMap<>();
    
    /**
     * Store delivery parameters for an order (in memory only)
     */
    public void storeDeliveryParams(UUID orderId, CreateOrderRequest deliveryParams) {
        if (deliveryParams != null) {
            deliveryParamsCache.put(orderId, deliveryParams);
            log.info("Stored delivery params for order: {}", orderId);
        }
    }
    
    /**
     * Retrieve and remove delivery parameters for an order
     */
    private CreateOrderRequest getAndRemoveDeliveryParams(UUID orderId) {
        CreateOrderRequest params = deliveryParamsCache.remove(orderId);
        log.info("Retrieved and removed delivery params for order: {}", orderId);
        return params;
    }

   /**
    * Step 1: Initiate saga by publishing payment request
    */
   @Transactional
   public void initiateOrderSaga(Order order) {
       log.info("Initiating saga for order: {}", order.getId());

       // Update order status to PENDING
       order.setStatus(OrderStatus.PENDING);
       orderRepository.save(order);

       // Publish payment request event
       OrderPaymentRequestEvent paymentRequest = OrderPaymentRequestEvent.builder()
               .orderId(order.getId())
               .userId(order.getUserId())
               .amount(order.getTotalPrice())
               .correlationId(order.getId().toString())
               .timestamp(System.currentTimeMillis())
               .build();

       kafkaTemplate.send(KafkaTopics.ORDER_PAYMENT_REQUEST, order.getId().toString(), paymentRequest);
       log.info("Published payment request for order: {}", order.getId());
   }

    /**
     * Step 2: Handle successful payment - reserve inventory
     */
    @Transactional
    public void processPaymentSuccess(UUID orderId, String transactionId) {
        log.info("Payment successful for orderId {} with txn {}", orderId, transactionId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Update order status
        order.setStatus(OrderStatus.PAYMENT_COMPLETED);
        orderRepository.save(order);
        
        // Publish inventory reserve request
        OrderInventoryReserveRequestEvent inventoryRequest = OrderInventoryReserveRequestEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .items(order.getItems().stream()
                        .map(item -> OrderInventoryReserveRequestEvent.InventoryItem.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .correlationId(order.getId().toString())
                .timestamp(System.currentTimeMillis())
                .build();
        
        kafkaTemplate.send(KafkaTopics.ORDER_INVENTORY_RESERVE_REQUEST, String.valueOf(orderId), inventoryRequest);
        log.info("Published inventory reserve request for order: {}", orderId);
    }

    /**
     * Step 3: Handle inventory reservation success - create shipment
     */
    @Transactional
    public void processInventoryReserveSuccess(UUID orderId) {
        log.info("Inventory reserved successfully for orderId {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Update order status
        order.setStatus(OrderStatus.INVENTORY_RESERVED);
        orderRepository.save(order);
        
        // Retrieve delivery params from cache (if provided by client)
        CreateOrderRequest deliveryParams = getAndRemoveDeliveryParams(orderId);
        
        // Build shipping request with delivery params if available, otherwise use defaults
        OrderShippingRequestEvent.OrderShippingRequestEventBuilder shippingRequestBuilder = OrderShippingRequestEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .orderAmount(order.getTotalPrice())
                .correlationId(order.getId().toString())
                .timestamp(System.currentTimeMillis());
        
        // Use provided delivery params or defaults
        if (deliveryParams != null) {
            shippingRequestBuilder
                    .shippingAddress(deliveryParams.getShippingAddress() != null ? deliveryParams.getShippingAddress() : "Customer Address")
                    .city(deliveryParams.getCity() != null ? deliveryParams.getCity() : "City")
                    .state(deliveryParams.getState() != null ? deliveryParams.getState() : "State")
                    .zipCode(deliveryParams.getZipCode() != null ? deliveryParams.getZipCode() : "000000")
                    .country(deliveryParams.getCountry() != null ? deliveryParams.getCountry() : "Country")
                    .phoneNumber(deliveryParams.getPhoneNumber() != null ? deliveryParams.getPhoneNumber() : "0000000000");
            log.info("Using client-provided delivery params for order: {}", orderId);
        } else {
            shippingRequestBuilder
                    .shippingAddress("Customer Address")
                    .city("City")
                    .state("State")
                    .zipCode("000000")
                    .country("Country")
                    .phoneNumber("0000000000");
            log.info("Using default delivery params for order: {}", orderId);
        }
        
        OrderShippingRequestEvent shippingRequest = shippingRequestBuilder.build();
        
        kafkaTemplate.send(KafkaTopics.ORDER_SHIPPING_REQUEST, String.valueOf(orderId), shippingRequest);
        log.info("Published shipping request for order: {} with address: {}", orderId, shippingRequest.getShippingAddress());
    }

    /**
     * Step 4: Handle shipping success - complete order
     */
    @Transactional
    public void processShippingSuccess(UUID orderId, String shipmentId, String trackingNumber) {
        log.info("Shipping created successfully for orderId {} with shipmentId {}", orderId, shipmentId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Update order status to COMPLETED
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        
        // Publish notification request
        OrderNotificationRequestEvent notificationRequest = OrderNotificationRequestEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .userEmail("customer@example.com") // TODO: Get from user profile
                .notificationType("ORDER_CONFIRMED")
                .orderStatus("CONFIRMED")
                .orderAmount(order.getTotalPrice())
                .message("Your order has been confirmed and shipped. Tracking: " + trackingNumber)
                .correlationId(order.getId().toString())
                .timestamp(System.currentTimeMillis())
                .build();
        
        kafkaTemplate.send(KafkaTopics.ORDER_NOTIFICATION_REQUEST, String.valueOf(orderId), notificationRequest);
        log.info("Order saga completed successfully for order: {}. Notification sent.", orderId);
    }

    /**
     * Compensation: Handle payment failure - cancel order
     */
    @Transactional
    public void processPaymentFailure(UUID orderId, String transactionId) {
        log.info("Payment failed for orderId {} with txn {}, cancelling order", orderId, transactionId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Mark order as FAILED
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
        
        // Clean up delivery params cache
        deliveryParamsCache.remove(orderId);
        
        log.info("Order {} marked as FAILED due to payment failure", orderId);
    }

    /**
     * Compensation: Handle inventory failure - refund payment and cancel order
     */
    @Transactional
    public void processInventoryReserveFailure(UUID orderId, String reason) {
        log.info("Inventory reservation failed for orderId {}, initiating compensation", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Publish refund request
        PaymentRefundRequestEvent refundRequest = PaymentRefundRequestEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .transactionId("txn-" + orderId) // Should be stored from payment success
                .amount(order.getTotalPrice())
                .reason("INVENTORY_FAILED")
                .correlationId(order.getId().toString())
                .timestamp(System.currentTimeMillis())
                .build();
        
        kafkaTemplate.send(KafkaTopics.PAYMENT_REFUND_REQUEST, String.valueOf(orderId), refundRequest);
        log.info("Published refund request for order: {}", orderId);
        
        // Mark order as FAILED
        order.setStatus(OrderStatus.OUT_OF_STOCK);
        orderRepository.save(order);
        
        // Clean up delivery params cache
        deliveryParamsCache.remove(orderId);
        
        log.info("Order {} marked as OUT_OF_STOCK due to inventory failure", orderId);
    }

    /**
     * Compensation: Handle shipping failure - restore inventory, refund payment, cancel order
     */
    @Transactional
    public void processShippingFailure(UUID orderId, String reason) {
        log.info("Shipping failed for orderId {}, initiating full compensation", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // 1. Restore inventory
        InventoryRestoreRequestEvent restoreRequest = InventoryRestoreRequestEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .items(order.getItems().stream()
                        .map(item -> InventoryRestoreRequestEvent.RestoreItem.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .reason("SHIPPING_FAILED")
                .correlationId(order.getId().toString())
                .timestamp(System.currentTimeMillis())
                .build();
        
        kafkaTemplate.send(KafkaTopics.ORDER_INVENTORY_RESTORE_REQUEST, String.valueOf(orderId), restoreRequest);
        log.info("Published inventory restore request for order: {}", orderId);
        
        // 2. Refund payment
        PaymentRefundRequestEvent refundRequest = PaymentRefundRequestEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .transactionId("txn-" + orderId)
                .amount(order.getTotalPrice())
                .reason("SHIPPING_FAILED")
                .correlationId(order.getId().toString())
                .timestamp(System.currentTimeMillis())
                .build();
        
        kafkaTemplate.send(KafkaTopics.PAYMENT_REFUND_REQUEST, String.valueOf(orderId), refundRequest);
        log.info("Published refund request for order: {}", orderId);
        
        // 3. Mark order as CANCELLED
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
        
        log.info("Order {} marked as CANCELLED due to shipping failure", orderId);
    }

    /**
     * Handle successful payment refund
     */
    @Transactional
    public void processPaymentRefundSuccess(UUID orderId, String transactionId) {
        log.info("Payment refund successful for orderId {} with txn {}", orderId, transactionId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Update order status to REFUNDED
        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);
        
        log.info("Order {} marked as REFUNDED successfully", orderId);
    }

    /**
     * Handle failed payment refund
     */
    @Transactional
    public void processPaymentRefundFailure(UUID orderId, String reason) {
        log.error("Payment refund failed for orderId {}, reason: {}", orderId, reason);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Mark order with refund failure status - may need manual intervention
        order.setStatus(OrderStatus.REFUND_FAILED);
        orderRepository.save(order);
        
        // Publish notification for manual intervention
        OrderNotificationRequestEvent notificationRequest = OrderNotificationRequestEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .userEmail("admin@openshop.com") // Admin notification
                .notificationType("REFUND_FAILED")
                .orderStatus("REFUND_FAILED")
                .orderAmount(order.getTotalPrice())
                .message("URGENT: Payment refund failed for order " + orderId + ". Reason: " + reason + ". Manual intervention required.")
                .correlationId(order.getId().toString())
                .timestamp(System.currentTimeMillis())
                .build();
        
        kafkaTemplate.send(KafkaTopics.ORDER_NOTIFICATION_REQUEST, String.valueOf(orderId), notificationRequest);
        
        log.info("Order {} marked as REFUND_FAILED, notification sent for manual intervention", orderId);
    }

    /**
     * Handle user-initiated order cancellation
     */
    @Transactional
    public void cancelOrder(UUID orderId, Long userId, String reason) {
        log.info("Processing cancellation request for orderId {} by userId {}", orderId, userId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Verify the order belongs to the requesting user
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Order does not belong to user " + userId);
        }
        
        // Check if order can be cancelled based on current status
        OrderStatus currentStatus = order.getStatus();
        
        if (currentStatus == OrderStatus.CANCELLED || 
            currentStatus == OrderStatus.REFUNDED || 
            currentStatus == OrderStatus.FAILED) {
            throw new RuntimeException("Order " + orderId + " is already in " + currentStatus + " status and cannot be cancelled");
        }
        
        // If order is confirmed/shipped, it cannot be cancelled
        if (currentStatus == OrderStatus.CONFIRMED) {
            throw new RuntimeException("Order " + orderId + " has been confirmed/shipped and cannot be cancelled. Please contact support.");
        }
        
        log.info("Order {} is in {} status, proceeding with cancellation", orderId, currentStatus);
        
        // Based on order status, perform appropriate compensation actions
        if (currentStatus == OrderStatus.INVENTORY_RESERVED) {
            // Need to restore inventory and refund payment
            log.info("Order {} has inventory reserved, restoring inventory and refunding payment", orderId);
            
            // 1. Restore inventory
            InventoryRestoreRequestEvent restoreRequest = InventoryRestoreRequestEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .items(order.getItems().stream()
                            .map(item -> InventoryRestoreRequestEvent.RestoreItem.builder()
                                    .productId(item.getProductId())
                                    .quantity(item.getQuantity())
                                    .build())
                            .collect(Collectors.toList()))
                    .reason("USER_CANCELLED")
                    .correlationId(order.getId().toString())
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            kafkaTemplate.send(KafkaTopics.ORDER_INVENTORY_RESTORE_REQUEST, String.valueOf(orderId), restoreRequest);
            log.info("Published inventory restore request for cancelled order: {}", orderId);
            
            // 2. Refund payment
            PaymentRefundRequestEvent refundRequest = PaymentRefundRequestEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .transactionId("txn-" + orderId)
                    .amount(order.getTotalPrice())
                    .reason("USER_CANCELLED")
                    .correlationId(order.getId().toString())
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            kafkaTemplate.send(KafkaTopics.PAYMENT_REFUND_REQUEST, String.valueOf(orderId), refundRequest);
            log.info("Published refund request for cancelled order: {}", orderId);
            
        } else if (currentStatus == OrderStatus.PAYMENT_COMPLETED) {
            // Only need to refund payment
            log.info("Order {} has payment completed, refunding payment", orderId);
            
            PaymentRefundRequestEvent refundRequest = PaymentRefundRequestEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .transactionId("txn-" + orderId)
                    .amount(order.getTotalPrice())
                    .reason("USER_CANCELLED")
                    .correlationId(order.getId().toString())
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            kafkaTemplate.send(KafkaTopics.PAYMENT_REFUND_REQUEST, String.valueOf(orderId), refundRequest);
            log.info("Published refund request for cancelled order: {}", orderId);
        }
        
        // Mark order as CANCELLED
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        // Send cancellation notification
        OrderNotificationRequestEvent notificationRequest = OrderNotificationRequestEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .userEmail("customer@example.com") // TODO: Get from user profile
                .notificationType("ORDER_CANCELLED")
                .orderStatus("CANCELLED")
                .orderAmount(order.getTotalPrice())
                .message("Your order has been cancelled. " + (reason != null ? "Reason: " + reason : "") + " Refund will be processed within 5-7 business days.")
                .correlationId(order.getId().toString())
                .timestamp(System.currentTimeMillis())
                .build();
        
        kafkaTemplate.send(KafkaTopics.ORDER_NOTIFICATION_REQUEST, String.valueOf(orderId), notificationRequest);
        
        log.info("Order {} cancelled successfully by user {}", orderId, userId);
    }
}
