package com.openshop.productservice.graphql.type;

import java.util.List;

public record ProductConnection(
    List<ProductEdge> edges,
    PageInfo pageInfo,
    Integer totalCount
) {
}
