package com.openshop.productservice.graphql.input;

import com.openshop.productservice.model.ProductStatus;

public record ProductFilter(
    String category,
    ProductStatus status,
    Double minPrice,
    Double maxPrice,
    String searchTerm
) {
}
