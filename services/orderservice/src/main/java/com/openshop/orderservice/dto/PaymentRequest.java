package com.openshop.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "User ID is required")
    private Long userId;


    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(PENDING|INITIATED)$", message = "Status must be either PENDING or INITIATED")
    private String status;

    private Double amount;
}
