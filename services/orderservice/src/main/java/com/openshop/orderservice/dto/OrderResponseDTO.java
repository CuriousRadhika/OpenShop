package com.openshop.orderservice.dto;


import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for order responses
 * Separate response DTO to avoid entity exposure
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {

    private UUID id;
    private Long userId;
    private String status;
    private Double totalPrice;
    private String currency;
    private List<OrderItemDTO> items;
    private String checkoutBatchId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}