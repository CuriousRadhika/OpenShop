package com.openshop.userservice.controller;


import com.openshop.userservice.dto.UserDTO;

import com.openshop.userservice.exception.UserNotFoundException;
import com.openshop.userservice.mapper.UserMapper;
import com.openshop.userservice.model.User;
import com.openshop.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller for managing User-related administrative operations.
 * Only accessible to users with ADMIN role.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;




    /**
     * API to fetch the currently logged-in user's profile details.
     *
     * @param authentication Spring Security Authentication object (auto-injected).
     * @return User details if found, otherwise HTTP 404.
     */
    @Operation(summary = "Get My User Details", description = "Fetches the profile details of the currently logged-in user.")
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMyDetails(Authentication authentication) {
        String username = authentication.getName();
        log.info("Fetching profile for logged-in user: {}", username);

        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(UserMapper.toDTO(user)))
                .orElseThrow(() -> new UserNotFoundException("User with username '" + username + "' not found"));
    }


    /**
     * Update an existing user
     *
     * @param user User object with updated data
     * @return Updated UserDTO
     */
    @PutMapping("/me")
    @Operation(summary = "Update user information ")
    public ResponseEntity<UserDTO> updateUser(Authentication authentication,@Valid @RequestBody User user) {
        String username = authentication.getName(); // Get the username of the logged-in user
        log.info("Fetching profile for logged-in user: {}", username);

        Optional<User> existingUser = userService.findByUsername(username);
        if (existingUser.isEmpty()) {
            throw new UserNotFoundException("Cannot update. User not found");
        }
        else{
            User updatedUser = userService.updateUser(existingUser.get().getId(),user);
            return ResponseEntity.ok(UserMapper.toDTO(updatedUser));
        }


    }
}
