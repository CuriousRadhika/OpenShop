package com.openshop.cartservice.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDTO {
    private UUID productId;
    private int quantity;
    private double price;
}
