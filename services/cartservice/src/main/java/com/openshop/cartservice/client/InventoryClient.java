package com.openshop.cartservice.client;

import com.openshop.cartservice.dto.InventoryRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "inventory-service", url = "${inventory.service.url:http://inventory-service:8086}")
public interface InventoryClient {

    @GetMapping("/api/inventory/{productId}")
    InventoryRequest getInventoryByProductId(
            @PathVariable("productId") UUID productId
    );

}
