package com.openshop.inventoryservice.client;

import com.openshop.inventoryservice.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;


import java.util.UUID;

@FeignClient(name = "product-service", url = "${product.service.url:http://product-service:8082}")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductResponse getProductById(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Service-Name") String serviceName
    );
}
