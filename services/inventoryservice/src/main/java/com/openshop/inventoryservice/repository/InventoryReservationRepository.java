package com.openshop.inventoryservice.repository;

import com.openshop.inventoryservice.model.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    
    Optional<InventoryReservation> findByCorrelationId(String correlationId);
    
    Optional<InventoryReservation> findByOrderId(UUID orderId);
}
