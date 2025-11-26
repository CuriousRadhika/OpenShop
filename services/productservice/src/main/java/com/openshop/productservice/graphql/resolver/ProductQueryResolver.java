package com.openshop.productservice.graphql.resolver;

import com.openshop.productservice.graphql.input.ProductFilter;
import com.openshop.productservice.graphql.input.ProductSort;
import com.openshop.productservice.graphql.type.ProductConnection;
import com.openshop.productservice.model.Product;
import com.openshop.productservice.service.ProductService;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProductQueryResolver {

    private final ProductService productService;

    @QueryMapping
    public Product product(@Argument String id, DataFetchingEnvironment env) {
        log.info("GraphQL Query: product(id={})", id);
        
        // Extract headers from GraphQL context
        Long userId = extractUserId(env);
        String role = extractUserRole(env);
        
        return productService.getProductById(UUID.fromString(id), userId, role);
    }

    @QueryMapping
    public ProductConnection products(
            @Argument ProductFilter filter,
            @Argument ProductSort sort,
            @Argument Integer first,
            @Argument String after,
            DataFetchingEnvironment env) {
        
        log.info("GraphQL Query: products(filter={}, sort={}, first={}, after={})", 
            filter, sort, first, after);
        
        // Extract headers from GraphQL context
        Long userId = extractUserId(env);
        String role = extractUserRole(env);
        
        // For now, return a simple list wrapped in connection format
        // TODO: Implement proper cursor-based pagination
        List<Product> allProducts = productService.getAllProducts(userId, role);
        
        return new ProductConnection(
            allProducts.stream()
                .map(p -> new com.openshop.productservice.graphql.type.ProductEdge(p, p.getId().toString()))
                .toList(),
            new com.openshop.productservice.graphql.type.PageInfo(false, false, null, null),
            allProducts.size()
        );
    }

    @QueryMapping
    public List<Product> myProducts(
            @Argument ProductFilter filter,
            @Argument ProductSort sort,
            DataFetchingEnvironment env) {
        
        log.info("GraphQL Query: myProducts(filter={}, sort={})", filter, sort);
        
        // Extract headers from GraphQL context
        Long userId = extractUserId(env);
        String role = extractUserRole(env);
        
        if (!"SELLER".equalsIgnoreCase(role)) {
            throw new com.openshop.productservice.exception.UnauthorizedException(
                "Only sellers can access myProducts");
        }
        
        return productService.getSellerProducts(userId);
    }

    @QueryMapping
    public ProductConnection searchProducts(
            @Argument String query,
            @Argument String category,
            @Argument Integer first,
            @Argument String after,
            DataFetchingEnvironment env) {
        
        log.info("GraphQL Query: searchProducts(query={}, category={}, first={}, after={})", 
            query, category, first, after);
        
        // Extract headers from GraphQL context
        Long userId = extractUserId(env);
        String role = extractUserRole(env);
        
        // For now, use getAllProducts and filter by search term
        // TODO: Implement proper search functionality
        List<Product> allProducts = productService.getAllProducts(userId, role);
        List<Product> filtered = allProducts.stream()
            .filter(p -> p.getName().toLowerCase().contains(query.toLowerCase()) ||
                        (p.getDescription() != null && p.getDescription().toLowerCase().contains(query.toLowerCase())))
            .toList();
        
        return new ProductConnection(
            filtered.stream()
                .map(p -> new com.openshop.productservice.graphql.type.ProductEdge(p, p.getId().toString()))
                .toList(),
            new com.openshop.productservice.graphql.type.PageInfo(false, false, null, null),
            filtered.size()
        );
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
