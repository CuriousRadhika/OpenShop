package com.openshop.productservice.graphql.type;

public record ErrorDetail(
    String field,
    String message,
    String code
) {
    public static ErrorDetail of(String field, String message) {
        return new ErrorDetail(field, message, null);
    }

    public static ErrorDetail of(String field, String message, String code) {
        return new ErrorDetail(field, message, code);
    }
}
