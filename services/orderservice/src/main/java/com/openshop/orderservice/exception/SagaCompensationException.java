package com.openshop.orderservice.exception;

public class SagaCompensationException extends RuntimeException {
    
    public SagaCompensationException(String message) {
        super(message);
    }
    
    public SagaCompensationException(String message, Throwable cause) {
        super(message, cause);
    }
}
