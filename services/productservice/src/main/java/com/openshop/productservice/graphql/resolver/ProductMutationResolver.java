package com.openshop.productservice.graphql.resolver;

import com.openshop.productservice.graphql.input.CreateProductInput;
import com.openshop.productservice.graphql.input.UpdateProductInput;
import com.openshop.productservice.graphql.type.ProductResponse;
import com.openshop.productservice.model.Product;
import com.openshop.productservice.service.ProductService;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProductMutationResolver {

    private final ProductService productService;

    @MutationMapping
    public ProductResponse createProduct(
            @Argument CreateProductInput input,
            DataFetchingEnvironment env) {
        
        log.info("GraphQL Mutation: createProduct(input={})", input);
        
        // Extract headers from GraphQL context
        Long userId = extractUserId(env);
        String role = extractUserRole(env);
        
        if (!"SELLER".equalsIgnoreCase(role)) {
            return ProductResponse.error("Only sellers can create products");
        }
        
        if (userId == null) {
            return ProductResponse.error("User ID is required");
        }
        
        try {
            Product product = input.toProduct(userId);
            Product created = productService.addProduct(product);
            return ProductResponse.success(created, "Product created successfully");
        } catch (Exception e) {
            log.error("Error creating product", e);
            return ProductResponse.error("Failed to create product: " + e.getMessage());
        }
    }

    @MutationMapping
    public ProductResponse updateProduct(
            @Argument String id,
            @Argument UpdateProductInput input,
            DataFetchingEnvironment env) {
        
        log.info("GraphQL Mutation: updateProduct(id={}, input={})", id, input);
        
        // Extract headers from GraphQL context
        Long userId = extractUserId(env);
        String role = extractUserRole(env);
        
        if (!"SELLER".equalsIgnoreCase(role)) {
            return ProductResponse.error("Only sellers can update products");
        }
        
        if (userId == null) {
            return ProductResponse.error("User ID is required");
        }
        
        try {
            // Build updated product from input
            Product updateData = Product.builder()
                .name(input.name())
                .description(input.description())
                .category(input.category())
                .price(input.price())
                .currency(input.currency())
                .sku(input.sku())
                .imageUrl(input.imageUrl())
                .status(input.status())
                .build();
            
            Product updated = productService.updateProduct(UUID.fromString(id), updateData, userId);
            return ProductResponse.success(updated, "Product updated successfully");
        } catch (com.openshop.productservice.exception.ProductNotFoundException e) {
            log.error("Product not found: {}", id, e);
            return ProductResponse.error("Product not found");
        } catch (com.openshop.productservice.exception.UnauthorizedException e) {
            log.error("Unauthorized update attempt: {}", id, e);
            return ProductResponse.error("You can only update your own products");
        } catch (Exception e) {
            log.error("Error updating product", e);
            return ProductResponse.error("Failed to update product: " + e.getMessage());
        }
    }

    @MutationMapping
    public ProductResponse deleteProduct(
            @Argument String id,
            DataFetchingEnvironment env) {
        
        log.info("GraphQL Mutation: deleteProduct(id={})", id);
        
        // Extract headers from GraphQL context
        Long userId = extractUserId(env);
        String role = extractUserRole(env);
        
        if (!"SELLER".equalsIgnoreCase(role)) {
            return ProductResponse.error("Only sellers can delete products");
        }
        
        if (userId == null) {
            return ProductResponse.error("User ID is required");
        }
        
        try {
            productService.deleteProduct(UUID.fromString(id), userId);
            return ProductResponse.success(null, "Product deleted successfully");
        } catch (com.openshop.productservice.exception.ProductNotFoundException e) {
            log.error("Product not found: {}", id, e);
            return ProductResponse.error("Product not found");
        } catch (com.openshop.productservice.exception.UnauthorizedException e) {
            log.error("Unauthorized delete attempt: {}", id, e);
            return ProductResponse.error("You can only delete your own products");
        } catch (Exception e) {
            log.error("Error deleting product", e);
            return ProductResponse.error("Failed to delete product: " + e.getMessage());
        }
    }

    private Long extractUserId(DataFetchingEnvironment env) {
        try {
            String userIdStr = env.getGraphQlContext().get("X-User-Id");
            return userIdStr != null ? Long.parseLong(userIdStr) : null;
        } catch (Exception e) {
            log.warn("Failed to extract user ID from context", e);
            return null;
        }
    }

    private String extractUserRole(DataFetchingEnvironment env) {
        try {
            return env.getGraphQlContext().get("X-User-Role");
        } catch (Exception e) {
            log.warn("Failed to extract user role from context", e);
            return null;
        }
    }
}
