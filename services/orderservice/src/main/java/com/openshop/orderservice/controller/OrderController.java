package com.openshop.orderservice.controller;


import com.openshop.orderservice.dto.CreateOrderRequest;
import com.openshop.orderservice.dto.OrderResponseDTO;
import com.openshop.orderservice.mapper.OrderMapper;
import com.openshop.orderservice.model.Order;

import com.openshop.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.UUID;


@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;




    @PostMapping
    public ResponseEntity<Object> createOrder(@RequestHeader("X-User-Id") Long userId,
                                              @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
                                              @RequestBody(required = false) CreateOrderRequest request)  {

        log.info("Checkout initiated for userId: {} with delivery params: {}", userId, request);
        Order order = orderService.createOrder(userId, idempotencyKey, request);
        if(order == null) {
            log.warn("Order creation failed for userId: {}", userId);
            return ResponseEntity.badRequest().body("Cart is empty. Cannot checkout.");
        }
        return ResponseEntity.ok(order);
    }


    @GetMapping("/user")
    public ResponseEntity<List<Order>> getOrdersByUserId(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        
        log.info("Fetching orders for userId: {}", userId);
        // Users can only view their own orders
        List<Order> orders = orderService.getOrdersByUserId(userId);
        log.info("Retrieved {} orders for userId: {}", orders.size(), userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@RequestHeader("X-User-Id") Long userId,
                                                         @RequestHeader(value = "X-User-Role", required = false) String role,
             @PathVariable UUID orderId) {


        log.info("Fetching order by ID: {}, userId: {}", orderId, userId);
        Order order = orderService.getOrderById(orderId);

        // Verify user owns the order (if not admin)
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        if (!isAdmin && userId != null && !order.getUserId().equals(userId)) {
            log.warn("Unauthorized access to order: {} by user: {}", orderId, userId);
            return ResponseEntity.status(403).build();
        }

        // Return DTO instead of entity
        OrderResponseDTO responseDTO = OrderMapper.toResponseDTO(order);

        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Object> cancelOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("Cancel order request received for orderId: {} by userId: {}", orderId, userId);
        
        try {
            String reason = "User requested cancellation";
            
            Order cancelledOrder = orderService.cancelOrder(orderId, userId, reason);
            log.info("Order {} cancelled successfully", orderId);
            
            return ResponseEntity.ok(cancelledOrder);
        } catch (RuntimeException e) {
            log.error("Failed to cancel order {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
