package com.openshop.productservice.graphql.type;

import com.openshop.productservice.model.Product;
import java.util.List;

public record ProductResponse(
    Boolean success,
    String message,
    Product product,
    List<ErrorDetail> errors
) {
    public static ProductResponse success(Product product) {
        return new ProductResponse(true, "Operation successful", product, null);
    }

    public static ProductResponse success(Product product, String message) {
        return new ProductResponse(true, message, product, null);
    }

    public static ProductResponse error(String message) {
        return new ProductResponse(false, message, null, null);
    }

    public static ProductResponse error(String message, List<ErrorDetail> errors) {
        return new ProductResponse(false, message, null, errors);
    }
}
