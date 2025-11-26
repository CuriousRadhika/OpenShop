package com.openshop.shippingservice.controller;

import com.openshop.shippingservice.model.Shipment;
import com.openshop.shippingservice.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping
    public Shipment createShipment(@RequestParam UUID orderId,
                                   @RequestHeader("X-User-Id") Long userId,
                                   @RequestParam String address) {
        return shippingService.createShipment(orderId, userId, address);
    }

    @GetMapping("/{orderId}")
    public Shipment getShipment(@PathVariable UUID orderId) {
        return shippingService.getShipmentByOrderId(orderId);
    }

    @PutMapping("/{shipmentId}/status")
    public Shipment updateStatus(@PathVariable UUID shipmentId, @RequestParam String status) {
        return shippingService.updateShipmentStatus(shipmentId, status);
    }
}
