package com.openshop.shippingservice.service;

import com.openshop.shippingservice.model.Shipment;
import com.openshop.shippingservice.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShippingService {

    private final ShipmentRepository shipmentRepository;

    public Shipment createShipment(UUID orderId, Long userId, String address) {
        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .userId(userId)
                .address(address)
                .trackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .build();
        return shipmentRepository.save(shipment);
    }

    public Shipment getShipmentByOrderId(UUID orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Shipment not found for order: " + orderId));
    }

    public Shipment updateShipmentStatus(UUID shipmentId, String status) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found with ID: " + shipmentId));
        shipment.setStatus(status);
        return shipmentRepository.save(shipment);
    }
}
