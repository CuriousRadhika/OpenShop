package com.openshop.orderservice.dto;


import lombok.*;

import java.util.UUID;

/**
 * DTO for order items in responses
 * Separate DTO to avoid entity exposure
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {

    private UUID productId;
    private String productName;
    private Integer quantity;
    private Double price;
    private Double subtotal;
}