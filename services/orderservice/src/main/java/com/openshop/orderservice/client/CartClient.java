package com.openshop.orderservice.client;

import com.openshop.orderservice.dto.CartDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "cart-service", url = "${cart.service.url:http://cart-service:8085}")
public interface CartClient {

    @GetMapping("/api/cart")
    CartDTO getCart(@RequestHeader("X-User-Id") Long userId);

    @DeleteMapping("/api/cart/items")
    CartDTO clearCart(@RequestHeader("X-User-Id") Long userId);
}
