package com.openshop.productservice.graphql.type;

public record PageInfo(
    Boolean hasNextPage,
    Boolean hasPreviousPage,
    String startCursor,
    String endCursor
) {
}
