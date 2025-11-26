package com.openshop.productservice.graphql.input;

public record ProductSort(
    ProductSortField field,
    SortDirection direction
) {
    public enum ProductSortField {
        NAME,
        PRICE,
        CREATED_AT,
        UPDATED_AT
    }

    public enum SortDirection {
        ASC,
        DESC
    }
}
