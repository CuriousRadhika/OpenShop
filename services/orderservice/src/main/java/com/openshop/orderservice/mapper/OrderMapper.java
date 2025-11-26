package com.openshop.orderservice.mapper;



import com.openshop.orderservice.dto.OrderItemDTO;
import com.openshop.orderservice.dto.OrderResponseDTO;
import com.openshop.orderservice.model.Order;
import com.openshop.orderservice.model.OrderItem;

import java.util.stream.Collectors;

/**
 * Mapper utility for converting Order entities to DTOs
 * Proper entity-to-DTO conversion
 */
public class OrderMapper {

    private OrderMapper() {
        // Utility class, no instantiation
    }

    /**
     * Convert Order entity to OrderResponseDTO
     */
    public static OrderResponseDTO toResponseDTO(Order order) {
        if (order == null) {
            return null;
        }

        return OrderResponseDTO.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(String.valueOf(order.getStatus()))
                .totalPrice(order.getTotalPrice())
                .currency("USD") // Default currency
                .items(order.getItems() != null ?
                        order.getItems().stream()
                                .map(OrderMapper::toItemDTO)
                                .collect(Collectors.toList()) : null)
                .checkoutBatchId(order.getCheckoutBatchId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Convert OrderItem entity to OrderItemDTO
     */
    public static OrderItemDTO toItemDTO(OrderItem item) {
        if (item == null) {
            return null;
        }

        return OrderItemDTO.builder()
                .productId(item.getProductId())
                .productName(null) // Can be enriched from Product service if needed
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subtotal(item.getPrice() * item.getQuantity())
                .build();
    }
}