package com.openshop.productservice.graphql.input;

import com.openshop.productservice.model.Product;
import com.openshop.productservice.model.ProductStatus;

public record CreateProductInput(
    String name,
    String description,
    String category,
    Double price,
    String currency,
    String sku,
    String imageUrl
) {
    public Product toProduct(Long sellerId) {
        return Product.builder()
            .name(name)
            .description(description)
            .category(category)
            .price(price)
            .currency(currency)
            .sku(sku)
            .imageUrl(imageUrl)
            .sellerId(sellerId)
            .status(ProductStatus.ACTIVE)
            .build();
    }
}
