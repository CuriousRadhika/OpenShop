package com.openshop.productservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "inventory-service", url = "${inventory.service.url:http://inventory-service:8086}")
public interface InventoryClient {

    @PostMapping("/api/inventory")
    void updateInventory(
            @RequestBody InventoryRequest request,
            @RequestHeader("X-Service-Name") String serviceName);

    record InventoryRequest(UUID productId, int quantity) {}
}
