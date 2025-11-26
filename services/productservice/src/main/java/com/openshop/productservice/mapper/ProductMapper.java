package com.openshop.productservice.mapper;


import com.openshop.productservice.dto.ProductResponseDTO;
import com.openshop.productservice.model.Product;

/**
 * Mapper utility for converting Product entities to DTOs
 * Proper entity-to-DTO conversion
 */
public class ProductMapper {

    private ProductMapper() {
        // Utility class, no instantiation
    }

    /**
     * Convert Product entity to ProductResponseDTO
     */
    public static ProductResponseDTO toResponseDTO(Product product) {
        if (product == null) {
            return null;
        }

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .price(product.getPrice())
                .currency(product.getCurrency())
                .sku(product.getSku())
                .imageUrl(product.getImageUrl())
                .status(product.getStatus())
                .sellerId(product.getSellerId())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}

