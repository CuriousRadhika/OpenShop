package com.openshop.shippingservice.service;

import com.openshop.events.constants.KafkaTopics;
import com.openshop.events.order.OrderShippingRequestEvent;
import com.openshop.events.shipping.ShippingResponseEvent;
import com.openshop.shippingservice.model.Shipment;
import com.openshop.shippingservice.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingRequestConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ShipmentRepository shipmentRepository;

    @KafkaListener(topics = KafkaTopics.ORDER_SHIPPING_REQUEST, groupId = "shipping-service-group")
    public void consumeShippingRequest(OrderShippingRequestEvent event) {
        log.info("Received shipping request for orderId: {}", event.getOrderId());

        try {
            // Create and save shipment
            String trackingNumber = "TRACK-" + System.currentTimeMillis();

            Shipment shipment = Shipment.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .address(event.getShippingAddress())
                    .trackingNumber(trackingNumber)
                    .status("CREATED")
                    .createdAt(LocalDateTime.now())
                    .build();

            // Save shipment to database
            Shipment savedShipment = shipmentRepository.save(shipment);
            log.info("Saved shipment {} for order: {} to address: {}", savedShipment.getId(), event.getOrderId(), event.getShippingAddress());

            // Publish success response
            ShippingResponseEvent response = ShippingResponseEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .shipmentId(savedShipment.getId())
                    .status("SUCCESS")
                    .trackingNumber(trackingNumber)
                    .failureReason(null)
                    .correlationId(event.getCorrelationId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(KafkaTopics.SHIPPING_ORDER_RESPONSE, event.getOrderId().toString(), response);
            log.info("Published shipping response for orderId: {} with tracking: {}", event.getOrderId(), trackingNumber);

        } catch (Exception e) {
            log.error("Error processing shipping request for orderId: {}", event.getOrderId(), e);
            
            // Publish failure response
            ShippingResponseEvent response = ShippingResponseEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .shipmentId(null)
                    .status("FAILED")
                    .trackingNumber(null)
                    .failureReason("Shipping processing error: " + e.getMessage())
                    .correlationId(event.getCorrelationId())
                    .timestamp(System.currentTimeMillis())
                    .build();



            kafkaTemplate.send(KafkaTopics.SHIPPING_ORDER_RESPONSE, event.getOrderId().toString(), response);
        }
    }
}
