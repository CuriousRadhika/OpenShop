package com.openshop.shippingservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID orderId;
    private Long userId;

    private String address;
    private String trackingNumber;
    private String status; // CREATED, IN_TRANSIT, DELIVERED

    private LocalDateTime createdAt;
}
