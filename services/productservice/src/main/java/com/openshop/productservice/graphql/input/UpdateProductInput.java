package com.openshop.productservice.graphql.input;

import com.openshop.productservice.model.ProductStatus;

public record UpdateProductInput(
    String name,
    String description,
    String category,
    Double price,
    String currency,
    String sku,
    String imageUrl,
    ProductStatus status
) {
}
