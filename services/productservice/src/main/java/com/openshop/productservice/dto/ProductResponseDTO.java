package com.openshop.productservice.dto;



import com.openshop.productservice.model.ProductStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for product responses
 * Separate response DTO to avoid entity exposure
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDTO {

    private UUID id;
    private String name;
    private String description;
    private String category;
    private Double price;
    private String currency;
    private String sku;
    private String imageUrl;
    private ProductStatus status;
    private Long sellerId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
