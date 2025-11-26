package com.openshop.userservice.service;



import com.openshop.userservice.model.User;
import com.openshop.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom implementation of UserDetailsService to load user details from the database
 * and return a custom UserDetailsImpl instance for authentication.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    /**
     * Constructor to initialize the UserRepository for fetching user data.
     *
     * @param userRepository Repository to interact with the User entity.
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user details by username from the database.
     * If the user is not found, a UsernameNotFoundException is thrown.
     *
     * @param username The username of the user to load.
     * @return UserDetails containing user information for authentication.
     * @throws UsernameNotFoundException If the user is not found.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Attempting to load user details for username: {}", username);

        // Fetch the user from the repository
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        logger.info("User found with username: {}", username);
        return new UserDetailsImpl(user);  // Returning the custom UserDetailsImpl
    }
}