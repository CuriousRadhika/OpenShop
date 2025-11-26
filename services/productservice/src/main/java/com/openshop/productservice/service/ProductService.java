package com.openshop.productservice.service;

import com.openshop.productservice.client.InventoryClient;
import com.openshop.productservice.exception.ProductNotFoundException;
import com.openshop.productservice.exception.UnauthorizedException;
import com.openshop.productservice.model.Product;
import com.openshop.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService{

    private final ProductRepository productRepository;
    private final InventoryClient inventoryClient;

    public Product addProduct(Product product) {
        log.info("Adding new product: name={}, category={}, price={}, sellerId={}", 
                 product.getName(), product.getCategory(), product.getPrice(), product.getSellerId());
        
        Product saved = productRepository.save(product);
        log.debug("Product saved to database with ID: {}", saved.getId());

        try {
            log.info("Creating inventory record for product: {}", saved.getId());
            inventoryClient.updateInventory(
                    new InventoryClient.InventoryRequest(saved.getId(), 0),
                    "product-service");
            log.info("Inventory record created successfully for product: {}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to create inventory record for product {}: {}", saved.getId(), e.getMessage(), e);
        }

        log.info("Product added successfully: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    public Product updateProduct(UUID id, Product updated, Long sellerId) {
        log.info("Updating product: id={}, sellerId={}", id, sellerId);
        
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found for update: id={}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });
        
        log.debug("Found existing product: id={}, currentSellerId={}", existing.getId(), existing.getSellerId());

        if (!existing.getSellerId().equals(sellerId)) {
            log.warn("Unauthorized update attempt: product={}, owner={}, requester={}", 
                     id, existing.getSellerId(), sellerId);
            throw new UnauthorizedException("Unauthorized: you can only modify your own products");
        }

        log.debug("Updating product fields: price={}, description={}, category={}, status={}", 
                  updated.getPrice(), updated.getDescription(), updated.getCategory(), updated.getStatus());
        
        existing.setPrice(updated.getPrice());
        existing.setDescription(updated.getDescription());
        existing.setCategory(updated.getCategory());
        existing.setImageUrl(updated.getImageUrl());
        existing.setStatus(updated.getStatus());

        Product savedProduct = productRepository.save(existing);
        
        log.info("Product updated successfully: id={}, name={}, price={}", 
                 savedProduct.getId(), savedProduct.getName(), savedProduct.getPrice());
        
        return savedProduct;
    }

    public void deleteProduct(UUID id, Long sellerId) {
        log.info("Deleting product: id={}, sellerId={}", id, sellerId);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found for deletion: id={}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });
        
        if (!product.getSellerId().equals(sellerId)) {
            log.warn("Unauthorized delete attempt: product={}, owner={}, requester={}", 
                     id, product.getSellerId(), sellerId);
            throw new UnauthorizedException("Unauthorized delete attempt");
        }
        
        productRepository.delete(product);
        
        log.info("Product deleted successfully: id={}, name={}", id, product.getName());
    }

    public List<Product> getAllProducts(Long userId, String role) {
        log.info("Retrieving all products: userId={}, role={}", userId, role);
        
        if ("SELLER".equalsIgnoreCase(role) && userId != null) {
            log.debug("Seller access: returning products for sellerId={}", userId);
            List<Product> sellerProducts = productRepository.findBySellerId(userId);
            log.info("Found {} products for seller {}", sellerProducts.size(), userId);
            return sellerProducts;
        }
        
        log.debug("Public access: returning all ACTIVE products");
        List<Product> activeProducts = productRepository.findAll().stream()
                .filter(p -> "ACTIVE".equals(p.getStatus().name()))
                .collect(Collectors.toList());
        
        log.info("Found {} ACTIVE products for public access", activeProducts.size());
        return activeProducts;
    }

    public List<Product> getSellerProducts(Long sellerId) {
        log.info("Retrieving products for seller: {}", sellerId);
        
        List<Product> products = productRepository.findBySellerId(sellerId);
        
        log.info("Found {} products for seller {}", products.size(), sellerId);
        log.debug("Seller {} products: {}", sellerId, products);
        
        return products;
    }

    public Product getProductById(UUID id, Long userId, String role) {
        log.info("Retrieving product by ID: id={}, userId={}, role={}", id, userId, role);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found: id={}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });
        
        log.debug("Found product: id={}, name={}, status={}, sellerId={}", 
                  product.getId(), product.getName(), product.getStatus(), product.getSellerId());
        
        if ("SELLER".equalsIgnoreCase(role) && userId != null) {
            if (!product.getSellerId().equals(userId)) {
                log.warn("Seller {} attempted to view product {} owned by seller {}", 
                         userId, id, product.getSellerId());
                throw new UnauthorizedException("You can only view your own products");
            }
            log.debug("Seller {} accessing their own product {}", userId, id);
            return product;
        }
        
        if (!"ACTIVE".equals(product.getStatus().name())) {
            log.warn("Attempt to access non-ACTIVE product: id={}, status={}, userId={}, role={}", 
                     id, product.getStatus(), userId, role);
            throw new ProductNotFoundException("Product not found with id: " + id);
        }
        
        log.info("Product retrieved successfully: id={}, name={}", product.getId(), product.getName());
        return product;
    }
}
