package com.openshop.inventoryservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations", indexes = {
    @Index(name = "idx_order_id", columnList = "orderId"),
    @Index(name = "idx_correlation_id", columnList = "correlationId", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private UUID orderId;
    
    @Column(nullable = false, unique = true)
    private String correlationId;
    
    @Column(nullable = false)
    private String status; // RESERVED, RESTORED, FAILED
    
    @Column(length = 1000)
    private String details; // JSON or text details about reserved items
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
