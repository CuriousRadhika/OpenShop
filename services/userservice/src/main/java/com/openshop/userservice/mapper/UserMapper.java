package com.openshop.userservice.mapper;


import com.openshop.userservice.dto.UserDTO;
import com.openshop.userservice.model.Role;
import com.openshop.userservice.model.User;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserMapper {

    private UserMapper(){}


    // Logger to log the mapping process
    private static final Logger logger = LoggerFactory.getLogger(UserMapper.class);

    /**
     * Converts a User entity to a UserDTO.
     *
     * @param user The User entity to be converted.
     * @return The corresponding UserDTO.
     */
    public static UserDTO toDTO(@NotNull User user) {
        logger.info("Mapping User entity to UserDTO for user with ID: {}", user.getId());

        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        logger.info("UserDTO created successfully for user with ID: {}", user.getId());
        return userDTO;
    }




    /**
     * Converts a UserDTO to a User entity.
     *
     * @param userDTO The UserDTO to be converted.
     * @return The corresponding User entity.
     */
    public static User toEntity(@NotNull UserDTO userDTO) {
        logger.info("Mapping UserDTO to User entity for user with ID: {}", userDTO.getId());

        User user = User.builder()
                .id(userDTO.getId())
                .name(userDTO.getName())
                .username(userDTO.getUsername())
                .email(userDTO.getEmail())
                .role(Role.valueOf(userDTO.getRole()))
                .build();

        logger.info("User entity created successfully for user with ID: {}", userDTO.getId());
        return user;
    }
}
