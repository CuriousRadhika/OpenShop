package com.openshop.inventoryservice.service;

import com.openshop.inventoryservice.client.ProductClient;
import com.openshop.inventoryservice.dto.InventoryRequest;
import com.openshop.inventoryservice.dto.ProductResponse;
import com.openshop.inventoryservice.model.Inventory;
import com.openshop.inventoryservice.repository.InventoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductClient productClient;

    public Optional<Inventory> getByProductId(UUID productId) {
        log.debug("Retrieving inventory for productId: {}", productId);
        return inventoryRepository.findByProductId(productId);
    }

    public Inventory createInventory(InventoryRequest req, boolean skipProductValidation,Long userId) {
        log.info("Creating inventory - productId: {}, quantity: {}, skipValidation: {}", 
                req.getProductId(), req.getQuantity(), skipProductValidation);
        
        // Only validate product exists if not called from product-service
        if (!skipProductValidation && !Objects.equals(productClient.getProductById(req.getProductId(), userId, "inventory-service").getStatus(), "ACTIVE")) {
            log.warn("Product validation failed - productId: {} does not exist", req.getProductId());
            throw new IllegalArgumentException("Cannot create inventory: Product does not exist with id " + req.getProductId());
        }
        
        Inventory inventory = Inventory.builder()
                .productId(req.getProductId())
                .quantity(req.getQuantity())
                .build();
        Inventory saved = inventoryRepository.save(inventory);
        log.info("Inventory created successfully - productId: {}, inventoryId: {}, quantity: {}", 
                req.getProductId(), saved.getId(), saved.getQuantity());
        return saved;
    }


    @Transactional
    public Inventory addStock(InventoryRequest req, Long userId, String role) {
        UUID pid = req.getProductId();
        log.info("Adding stock - productId: {}, quantity: {}, userId: {}, role: {}", 
                req.getProductId(), req.getQuantity(), userId, role);
        
        // Get product details including seller ID
        ProductResponse product = productClient.getProductById(req.getProductId(),userId,"inventory-service");
        if (product == null) {
            log.warn("Cannot add stock - product not found: {}", req.getProductId());
            throw new IllegalArgumentException("Cannot add stock: Product does not exist with id " + req.getProductId());
        }
        
        // If user is a SELLER, verify they own the product
        if ("SELLER".equalsIgnoreCase(role) && userId != null &&
                (product.getSellerId() == null || !product.getSellerId().equals(userId))) {
            log.warn("Unauthorized stock addition attempt - userId: {} does not own productId: {}", userId, req.getProductId());
            throw new RuntimeException("Unauthorized: You can only modify inventory for your own products");
        }

        
        Inventory inv = inventoryRepository.findByProductId(pid)
                .orElseGet(() -> {
                    log.info("Creating new inventory record for productId: {}", req.getProductId());
                    return Inventory.builder().productId(pid).quantity(0).build();
                });
        int previousQuantity = inv.getQuantity();
        inv.setQuantity(inv.getQuantity() + req.getQuantity());
        Inventory saved = inventoryRepository.save(inv);
        log.info("Stock added successfully - productId: {}, previousQuantity: {}, addedQuantity: {}, newQuantity: {}", 
                req.getProductId(), previousQuantity, req.getQuantity(), saved.getQuantity());
        return saved;
    }

    @Transactional
    public Inventory reduceStock(InventoryRequest req) {
        UUID pid = req.getProductId();
        log.info("Reducing stock - productId: {}, quantity: {}", req.getProductId(), req.getQuantity());
        
        Inventory inv = inventoryRepository.findByProductId(pid)
                .orElseThrow(() -> {
                    log.error("Cannot reduce stock - product not found in inventory: {}", req.getProductId());
                    return new RuntimeException("Product not found in inventory");
                });
        
        if (inv.getQuantity() < Math.abs(req.getQuantity())) {
            log.warn("Insufficient stock - productId: {}, available: {}, requested: {}", 
                    req.getProductId(), inv.getQuantity(), req.getQuantity());
            throw new RuntimeException("Insufficient stock");
        }
        
        int previousQuantity = inv.getQuantity();
        inv.setQuantity(inv.getQuantity() - Math.abs(req.getQuantity()));
        Inventory saved = inventoryRepository.save(inv);
        log.info("Stock reduced successfully - productId: {}, previousQuantity: {}, reducedQuantity: {}, newQuantity: {}", 
                req.getProductId(), previousQuantity, req.getQuantity(), saved.getQuantity());
        return saved;
    }

    /**
     * Reduce inventory for Kafka event - returns boolean for success/failure
     */
    @Transactional
    public boolean reduceInventory(UUID productId, Integer quantity) {
        log.info("Reducing inventory via Kafka - productId: {}, quantity: {}", productId, quantity);
        
        try {
            Inventory inv = inventoryRepository.findByProductId(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found in inventory"));
            
            if (inv.getQuantity() < quantity) {
                log.warn("Insufficient inventory - productId: {}, available: {}, requested: {}", 
                        productId, inv.getQuantity(), quantity);
                return false;
            }
            
            inv.setQuantity(inv.getQuantity() - quantity);
            inventoryRepository.save(inv);
            log.info("Inventory reduced successfully - productId: {}, newQuantity: {}", productId, inv.getQuantity());
            return true;
        } catch (Exception e) {
            log.error("Error reducing inventory for productId: {}", productId, e);
            return false;
        }
    }

    /**
     * Increase inventory for compensation/restore
     */
    @Transactional
    public void increaseInventory(String productId, Integer quantity) {
        log.info("Increasing inventory via Kafka - productId: {}, quantity: {}", productId, quantity);
        
        try {
            UUID pid = UUID.fromString(productId);
            Inventory inv = inventoryRepository.findByProductId(pid)
                    .orElseThrow(() -> new RuntimeException("Product not found in inventory"));
            
            inv.setQuantity(inv.getQuantity() + quantity);
            inventoryRepository.save(inv);
            log.info("Inventory increased successfully - productId: {}, newQuantity: {}", productId, inv.getQuantity());
        } catch (Exception e) {
            log.error("CRITICAL: Error increasing inventory for productId: {}", productId, e);
            throw e;
        }
    }
}
