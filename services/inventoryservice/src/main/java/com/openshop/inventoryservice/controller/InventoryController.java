package com.openshop.inventoryservice.controller;

import com.openshop.inventoryservice.dto.InventoryRequest;
import com.openshop.inventoryservice.exception.UnauthorizedException;
import com.openshop.inventoryservice.model.Inventory;
import com.openshop.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<Object> updateInventory(
            @Valid @RequestBody InventoryRequest req,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Service-Name", required = false) String serviceName) {
        
        log.info("Create inventory request - productId: {}, quantity: {}, role: {}, service: {}", 
                req.getProductId(), req.getQuantity(), role, serviceName);
        
        // Only allow SELLER role or internal service calls to create inventory
        if (!"SELLER".equalsIgnoreCase(role) && !"product-service".equalsIgnoreCase(serviceName) && !"order-service".equalsIgnoreCase(serviceName)) {
            log.warn("Unauthorized inventory creation attempt - role: {}, service: {}", role, serviceName);
            throw new UnauthorizedException("Only sellers or internal services can update inventory");
        }
        
        // Skip product validation if called from product-service to avoid circular dependency
        boolean skipValidation = "product-service".equalsIgnoreCase(serviceName);


        if (req.getQuantity() < 0) {
            try {
                Inventory updated = inventoryService.reduceStock(req);
                log.info("Stock reduced successfully - productId: {}, newQuantity: {}", req.getProductId(), updated.getQuantity());
                return ResponseEntity.ok(updated);
            } catch (RuntimeException e) {
                log.error("Failed to reduce stock - productId: {}, quantity: {} - Error: {}",
                        req.getProductId(), req.getQuantity(), e.getMessage());
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }
        else {
            // Verify if Inventory exists before adding stock
            Optional<Inventory> existingInventory = inventoryService.getByProductId(req.getProductId());
            // If not exists, create it first
            if(existingInventory.isEmpty()){
                Inventory created = inventoryService.createInventory(req, skipValidation, userId);
                log.info("Inventory created successfully - productId: {}, inventoryId: {}", req.getProductId(), created.getId());
            }

            Inventory updated = inventoryService.addStock(req, userId, role);
            log.info("Stock increased successfully - productId: {}, newQuantity: {}", req.getProductId(), updated.getQuantity());
            return ResponseEntity.ok(updated);
        }



    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryRequest> getInventory(@PathVariable UUID productId) {
        log.debug("Get inventory request - productId: {}", productId);

        return inventoryService.getByProductId(productId)
                .map(inventory -> {
                    log.debug("Inventory found - productId: {}, quantity: {}", productId, inventory.getQuantity());
                    return ResponseEntity.ok(InventoryRequest.builder().productId(inventory.getProductId()).quantity(inventory.getQuantity()).build());
                })
                .orElseGet(() -> {
                    log.debug("Inventory not found - productId: {}", productId);
                    return ResponseEntity.notFound().build();
                });
    }
}
