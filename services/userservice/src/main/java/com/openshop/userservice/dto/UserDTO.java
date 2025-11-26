package com.openshop.userservice.dto;


import lombok.*;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for User entity.
 * This class is used to transfer user data between different layers of the application.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    /**
     * Unique identifier for the user.
     */
    private Long id;

    /**
     * Full name of the user.
     */
    private String name;

    /**
     * Username used for authentication.
     */
    private String username;

    /**
     * Email address of the user.
     */
    private String email;

    /**
     * Role of the user (e.g., ADMIN, EMPLOYEE).
     */
    private String role;

    /**
     * Timestamp when the user was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the user was last updated.
     */
    private LocalDateTime updatedAt;

}