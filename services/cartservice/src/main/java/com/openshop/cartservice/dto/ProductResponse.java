package com.openshop.cartservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;


@Setter
@Getter
public class ProductResponse {
    private UUID id;
    private String name;
    private String description;
    private String category;
    private Double price;
    private String currency;
    private String sku;
    private Long sellerId;
    private String imageUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
