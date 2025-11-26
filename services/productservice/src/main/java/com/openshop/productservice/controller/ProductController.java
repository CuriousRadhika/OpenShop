package com.openshop.productservice.controller;

import com.openshop.productservice.dto.CreateProductRequestDTO;
import com.openshop.productservice.dto.ProductResponseDTO;
import com.openshop.productservice.exception.UnauthorizedException;
import com.openshop.productservice.mapper.ProductMapper;
import com.openshop.productservice.model.Product;
import com.openshop.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(
            @Valid @RequestBody CreateProductRequestDTO request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        
        log.info("Received request to create product: name={}, category={}, userId={}, role={}", 
                 request.getName(), request.getCategory(), userId, role);
        
        if (!"SELLER".equalsIgnoreCase(role)) {
            log.warn("Unauthorized product creation attempt by user {} with role {}", userId, role);
            throw new UnauthorizedException("Only sellers can create products");
        }

        // Convert DTO to entity
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .currency(request.getCurrency())
                .sku(request.getSku())
                .imageUrl(request.getImageUrl())
                .sellerId(userId)
                .build();
        
        product.setSellerId(userId);
        
        log.debug("Creating product for seller {}: {}", userId, product);
        Product createdProduct = productService.addProduct(product);
        
        log.info("Product created successfully: id={}, name={}, sellerId={}", 
                 createdProduct.getId(), createdProduct.getName(), createdProduct.getSellerId());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdProduct.getId())
                .toUri();

        // Return DTO instead of entity
        ProductResponseDTO responseDTO = ProductMapper.toResponseDTO(createdProduct);

        return ResponseEntity.created(location).body(responseDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProductRequestDTO request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        
        log.info("Received request to update product: id={}, userId={}, role={}", id, userId, role);
        
        if (!"SELLER".equalsIgnoreCase(role)) {
            log.warn("Unauthorized product update attempt by user {} with role {}", userId, role);
            throw new UnauthorizedException("Only sellers can update products");
        }

        Product productUpdate = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .currency(request.getCurrency())
                .sku(request.getSku())
                .imageUrl(request.getImageUrl())
                .build();
        
        log.debug("Updating product {} for seller {}", id, userId);
        Product updatedProduct = productService.updateProduct(id, productUpdate, userId);
        
        log.info("Product updated successfully: id={}, name={}, price={}", 
                 updatedProduct.getId(), updatedProduct.getName(), updatedProduct.getPrice());

        // Return DTO instead of entity
        ProductResponseDTO responseDTO = ProductMapper.toResponseDTO(updatedProduct);

        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        
        log.info("Received request to get product: id={}, userId={}, role={}", id, userId, role);
        
        Product product = productService.getProductById(id, userId, role);
        
        log.debug("Product retrieved: id={}, name={}, status={}", 
                  product.getId(), product.getName(), product.getStatus());
        
        return ResponseEntity.ok(product);
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        
        log.info("Received request to get all products: userId={}, role={}", userId, role);
        
        List<Product> products = productService.getAllProducts(userId, role);
        
        log.info("Retrieved {} products for user {} with role {}", products.size(), userId, role);
        log.debug("Products: {}", products);
        
        return ResponseEntity.ok(products);
    }
}
