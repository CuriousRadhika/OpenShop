package com.openshop.userservice.service;

import com.openshop.userservice.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Custom implementation of UserDetails to encapsulate user information for authentication.
 * This class integrates with Spring Security and uses roles assigned to the User entity
 * for handling authorization.
 */
public class UserDetailsImpl implements UserDetails {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsImpl.class);

    private final String username;
    private final String password;
    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Constructor to initialize UserDetailsImpl from a User entity.
     *
     * @param user The User entity from which to extract authentication details.
     */
    public UserDetailsImpl(User user) {
        this.user = user;
        this.username = user.getUsername();
        this.password = user.getPassword();
        // Creating a GrantedAuthority from the user's role


        List<GrantedAuthority> authorityList = new ArrayList<>();

        // Add the user's main role
        authorityList.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        // Add ROLE_USER if role is ADMIN, MANAGER, or FINANCE
        switch (user.getRole()) {
            case ADMIN:
                authorityList.add(new SimpleGrantedAuthority("ROLE_USER"));
                break;
            default:
                // No additional roles
                break;
        }

        this.authorities = authorityList;
        logger.info("UserDetailsImpl created for user: {}", username);
    }

    /**
     * Returns the authorities (roles) granted to the user.
     *
     * @return A collection of GrantedAuthority objects representing the user's roles.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Returns the user's password.
     *
     * @return The user's password.
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Returns the user's username.
     *
     * @return The username.
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Returns the full User entity for access to additional fields.
     *
     * @return The associated User entity.
     */
    public User getUser() {
        return user;
    }

    /**
     * Indicates whether the user's account has expired.
     *
     * @return true if the account is non-expired, otherwise false.
     */
    @Override
    public boolean isAccountNonExpired() {
        logger.debug("Checking if account is non-expired for user: {}", username);
        return true;
    }

    /**
     * Indicates whether the user's account is locked.
     *
     * @return true if the account is not locked, otherwise false.
     */
    @Override
    public boolean isAccountNonLocked() {
        logger.debug("Checking if account is non-locked for user: {}", username);
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) are expired.
     *
     * @return true if the credentials are valid (non-expired), otherwise false.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        logger.debug("Checking if credentials are non-expired for user: {}", username);
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled.
     *
     * @return true if the user is enabled, otherwise false.
     */
    @Override
    public boolean isEnabled() {
        logger.debug("Checking if account is enabled for user: {}", username);
        return user.isActive();
    }
}
