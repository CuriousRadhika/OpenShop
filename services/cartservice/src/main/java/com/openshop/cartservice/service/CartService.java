package com.openshop.cartservice.service;

import com.openshop.cartservice.client.InventoryClient;
import com.openshop.cartservice.client.ProductClient;
import com.openshop.cartservice.dto.ProductResponse;
import com.openshop.cartservice.exception.UnauthorizedException;
import com.openshop.cartservice.model.Cart;
import com.openshop.cartservice.model.CartItem;
import com.openshop.cartservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final ProductClient productClient;
    private final CartRepository cartRepository;
    private final InventoryClient inventoryClient;


    @Transactional
    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));
    }

    @Transactional
    public Cart addItem(Long userId, CartItem item) {
        // Fetch product details from productservice before adding to cart
        ProductResponse product = productClient.getProductById(item.getProductId(), userId, "cart-service");
        
        if (product == null || product.getStatus() == null || !product.getStatus().equals("ACTIVE")) {
            throw new IllegalArgumentException("Product not available or does not exist");
        }

        if(item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if(item.getQuantity() > inventoryClient.getInventoryByProductId(item.getProductId()).getQuantity()) {
            throw new IllegalArgumentException("Requested quantity exceeds available stock");
        }
        
        // Get cart once and reuse the same instance
        Cart cart = getCartByUserId(userId);
        
        // Check if item already exists in cart
        CartItem existingItem = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(item.getProductId()))
                .findFirst()
                .orElse(null);
        
        if (existingItem != null) {
            // Update quantity of existing item
            int newQuantity = existingItem.getQuantity() + item.getQuantity();
            
            // Validate new total quantity doesn't exceed available stock
            if(newQuantity > inventoryClient.getInventoryByProductId(item.getProductId()).getQuantity()) {
                throw new IllegalArgumentException("Requested quantity exceeds available stock");
            }
            
            existingItem.setQuantity(newQuantity);
        } else {
            // Add new item to cart
            item.setPrice(product.getPrice());
            item.setCart(cart); // Set bidirectional relationship
            cart.getItems().add(item);
        }
        
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItem(Long userId, CartItem item) {
        Cart cart = getCartByUserId(userId);
        
        // Check if quantity to remove is valid
        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity to remove must be greater than zero");
        }
        
        // Find the cart item (single stream operation)
        CartItem cartItem = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(item.getProductId()))
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("Cart item does not belong to your cart or does not exist"));

        // Check if quantity to remove exceeds current quantity
        if (item.getQuantity() > cartItem.getQuantity()) {
            throw new IllegalArgumentException("Quantity to remove exceeds current quantity in cart");
        }

        // Reduce quantity or remove item
        if (cartItem.getQuantity() > item.getQuantity()) {
            cartItem.setQuantity(cartItem.getQuantity() - item.getQuantity());
        } else {
            cart.getItems().remove(cartItem);
        }
        
        return cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        // Clear all items from the cart
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
