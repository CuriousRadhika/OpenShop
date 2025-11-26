package com.openshop.cartservice.controller;


import com.openshop.cartservice.dto.UpdateCartItemRequestDTO;
import com.openshop.cartservice.dto.CartDTO;
import com.openshop.cartservice.dto.CartItemDTO;
import com.openshop.cartservice.model.Cart;
import com.openshop.cartservice.model.CartItem;
import com.openshop.cartservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartDTO> getCart(@RequestHeader("X-User-Id") Long userId) {
        log.info("Fetching cart for userId: {}", userId);
        Cart cart = cartService.getCartByUserId(userId);
        log.debug("Cart retrieved for userId: {} with {} items", userId, cart.getItems() != null ? cart.getItems().size() : 0);
        return ResponseEntity.ok(CartDTO.builder()
                .userId(cart.getUserId())
                .items(
                        cart.getItems().stream()
                                .map(item -> CartItemDTO.builder()
                                       .productId(item.getProductId())
                                        .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .build())
                                .toList()
                ).build());
    }

    @PostMapping("/items")
    public ResponseEntity<CartDTO> updateCartItem(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody UpdateCartItemRequestDTO dto) {

        CartItem item = CartItem.builder()
                .productId(dto.getProductId())
                .quantity(dto.getQuantity())
                .build();

        if (item.getQuantity() < 0) {
            log.info("Removing item from cart for userId: {}, productId: {} due to non-positive quantity: {}", userId, item.getProductId(), item.getQuantity());
            item.setQuantity(-item.getQuantity()); // Make quantity positive for removal
            Cart cart = cartService.removeItem(userId, item);
            log.info("Item successfully removed from cart for userId: {}", userId);
            return ResponseEntity.ok(CartDTO.builder()
                    .userId(cart.getUserId())
                    .items(
                            cart.getItems().stream()
                                    .map(i -> CartItemDTO.builder()
                                            .productId(i.getProductId())
                                            .price(i.getPrice())
                                            .quantity(i.getQuantity())
                                            .build())
                                    .toList()
                    ).build());
        } else {
            log.info("Adding item to cart for userId: {}, productId: {}, quantity: {}", userId, item.getProductId(), item.getQuantity());
            Cart cart = cartService.addItem(userId, item);
            log.info("Item successfully added to cart for userId: {}", userId);
            return ResponseEntity.ok(CartDTO.builder()
                    .userId(cart.getUserId())
                    .items(
                            cart.getItems().stream()
                                    .map(i -> CartItemDTO.builder()
                                            .productId(i.getProductId())
                                            .price(i.getPrice())
                                            .quantity(i.getQuantity())
                                            .build())
                                    .toList()
                    ).build());
        }
    }

    @DeleteMapping("/items")
    public ResponseEntity<CartDTO> clearCart(@RequestHeader("X-User-Id") Long userId) {
        log.info("Clearing cart for userId: {}", userId);
        cartService.clearCart(userId);
        Cart cart = cartService.getCartByUserId(userId);
        CartDTO cartDTO = CartDTO.builder()
                .userId(cart.getUserId())
                .items(
                        cart.getItems().stream()
                                .map(item -> CartItemDTO.builder()
                                        .productId(item.getProductId())
                                        .price(item.getPrice())
                                        .quantity(item.getQuantity())
                                        .build())
                                .toList()
                ).build();
        log.info("Cart successfully cleared for userId: {}", userId);
        return ResponseEntity.ok(cartDTO);
    }
}
