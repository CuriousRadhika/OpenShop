package com.openshop.userservice.controller;

import com.openshop.userservice.dto.LoginRequestDTO;
import com.openshop.userservice.dto.LoginResponseDTO;
import com.openshop.userservice.dto.RegisterRequestDTO;
import com.openshop.userservice.jwt.JwtUtil;
import com.openshop.userservice.mapper.UserMapper;
import com.openshop.userservice.model.Role;
import com.openshop.userservice.model.User;
import com.openshop.userservice.service.UserDetailsImpl;
import com.openshop.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,UserService userService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Authenticates the user and returns a JWT token along with user details if successful.
     *
     * @param loginRequestDTO Object containing the username and password.
     * @return ResponseEntity with status and JWT token if authentication is successful, or error message.
     */
    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT token", description = "Authenticates the user with provided credentials and returns a JWT token with user details.")
    public ResponseEntity<Object> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        logger.info("Attempting to authenticate user with username: {}", loginRequestDTO.getUsername());

        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDTO.getUsername(),
                        loginRequestDTO.getPassword()
                )
        );

        // Generate JWT token

        String username = authentication.getName();
        User user = userService.findByUsername(username).orElseThrow(() -> {
            logger.error("User not found after authentication: {}", username);
            return new RuntimeException("User not found");
        });
        String token = jwtUtil.generateToken(username, String.valueOf(user.getRole()), user.getId());
        logger.info("Authentication successful for username: {}, userId: {}", loginRequestDTO.getUsername(), user.getId());



        // Fetch user details

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        LoginResponseDTO response = new LoginResponseDTO(token,jwtUtil.getTokenExpirationTime(token), UserMapper.toDTO(userDetails.getUser()));

        return ResponseEntity.ok(response);
    }

    /**
     * Registers a new user.
     *
     * @return ResponseEntity with status and message indicating the result of the registration.
     */
    @PostMapping("/register")
    public ResponseEntity<Object> register(@Valid @RequestBody RegisterRequestDTO dto) {
        // Use validated DTO instead of entity
        // Reduced logging
        logger.info("User registration attempt");

        // Check if the username already exists
        if (userService.findByUsername(dto.getUsername()).isPresent()) {
            logger.warn("Registration failed - username already exists");
            // Return 409 CONFLICT instead of 400 BAD REQUEST
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Username already exists"));
        }

        // Check if email already exists
        if (userService.existsByEmail(dto.getEmail())) {
            logger.warn("Registration failed - email already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already exists"));
        }



        // Convert DTO to entity
        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(dto.getPassword())
                .name(dto.getName())
                .role(Objects.equals(dto.getRole(), "CUSTOMER") || dto.getRole() == null ? Role.CUSTOMER : Role.SELLER) // Default role CUSTOMER
                .build();

        // Register the user
        User createdUser = userService.createUser(user);
        logger.info("User registered successfully");

        // Proper Location header
        URI locationHeaderUri = UriComponentsBuilder
                .fromPath("/api/users/{id}")
                .buildAndExpand(createdUser.getId())
                .toUri();

        return ResponseEntity.created(locationHeaderUri).body(UserMapper.toDTO(createdUser));
    }
}
