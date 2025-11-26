package com.openshop.paymentservice.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID orderId;
    private Long userId;
    private Double amount;
    private String status; // INITIATED, SUCCESS, FAILED, REFUNDED
    private String transactionId;
    private LocalDateTime timestamp;
    
    @Column(unique = true)
    private String idempotencyKey;
}
