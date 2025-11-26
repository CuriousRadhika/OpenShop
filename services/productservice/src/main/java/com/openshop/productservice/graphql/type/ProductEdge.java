package com.openshop.productservice.graphql.type;

import com.openshop.productservice.model.Product;

public record ProductEdge(
    Product node,
    String cursor
) {
}
