package com.openshop.orderservice.model;

public enum OrderStatus {
    PENDING,
    CREATED,
    PAYMENT_COMPLETED,
    INVENTORY_RESERVED,
    CONFIRMED,
    FAILED,
    OUT_OF_STOCK,
    CANCELLED,
    REFUNDED,
    REFUND_FAILED
}
