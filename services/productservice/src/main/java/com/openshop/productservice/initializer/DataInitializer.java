package com.openshop.productservice.initializer;



import com.openshop.productservice.model.Product;
import com.openshop.productservice.model.ProductStatus;
import com.openshop.productservice.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
public class DataInitializer {

    private final ProductRepository productRepository;


    public DataInitializer(
                           ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // Check if data already exists


            // Create Products for Seller 1 (Electronics)
            Product laptop = createProductIfNotExists(
                    "Gaming Laptop",
                    "High-performance gaming laptop with RTX 4060",
                    "electronics",
                    1299.99,
                    "USD",
                    "LAPTOP-001",
                    2L,
                    "https://example.com/laptop.jpg"
            );

            Product smartphone = createProductIfNotExists(
                    "Smartphone Pro",
                    "Latest flagship smartphone with 5G",
                    "electronics",
                    899.99,
                    "USD",
                    "PHONE-001",
                    2L,
                    "https://example.com/phone.jpg"
            );

            Product headphones = createProductIfNotExists(
                    "Wireless Headphones",
                    "Noise-cancelling wireless headphones",
                    "electronics",
                    199.99,
                    "USD",
                    "HEAD-001",
                    2L,
                    "https://example.com/headphones.jpg"
            );

            // Create Products for Seller 2 (Fashion)
            Product tshirt = createProductIfNotExists(
                    "Designer T-Shirt",
                    "Premium cotton designer t-shirt",
                    "clothing",
                    49.99,
                    "USD",
                    "TSHIRT-001",
                    2L,
                    "https://example.com/tshirt.jpg"
            );

            Product jeans = createProductIfNotExists(
                    "Slim Fit Jeans",
                    "Comfortable slim fit denim jeans",
                    "clothing",
                    79.99,
                    "USD",
                    "JEANS-001",
                    2L,
                    "https://example.com/jeans.jpg"
            );

            Product sneakers = createProductIfNotExists(
                    "Running Sneakers",
                    "Lightweight running sneakers",
                    "clothing",
                    129.99,
                    "USD",
                    "SNEAK-001",
                    2L,
                    "https://example.com/sneakers.jpg"
            );



            System.out.println("Database initialization completed successfully!");
            System.out.println("Created 2 sellers, 2 customers, 6 products, inventory entries, and 4 orders.");
        };
    }



    private Product createProductIfNotExists(String name, String description, String category,
                                             Double price, String currency, String sku,
                                             Long sellerId, String imageUrl) {
        // Check if product with same SKU exists
        Optional<Product> existingProduct = productRepository.findAll().stream()
                .filter(p -> p.getSku() != null && p.getSku().equals(sku))
                .findFirst();

        if (existingProduct.isPresent()) {
            System.out.println("Product already exists: " + name);
            return existingProduct.get();
        }

        Product product = Product.builder()
                .name(name)
                .description(description)
                .category(category)
                .price(price)
                .currency(currency)
                .sku(sku)
                .sellerId(sellerId)
                .imageUrl(imageUrl)
                .status(ProductStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Product savedProduct = productRepository.save(product);
        System.out.println("Product created: " + name);
        return savedProduct;
    }




}

