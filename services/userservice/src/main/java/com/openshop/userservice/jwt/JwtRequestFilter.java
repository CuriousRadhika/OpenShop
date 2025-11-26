package com.openshop.userservice.jwt;



import com.openshop.userservice.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    // Logger for the JwtRequestFilter class
    private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtRequestFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }
    /**
     * The main filter logic for validating JWT tokens and authenticating users.
     * This method checks if the request has a valid JWT token in the Authorization header,
     * and if valid, it authenticates the user by setting the authentication in the SecurityContext.
     *
     * @param request the HttpServletRequest
     * @param response the HttpServletResponse
     * @param chain the FilterChain to continue the request-response cycle
     * @throws ServletException if an error occurs during the filter processing
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,@NonNull FilterChain chain)
            throws ServletException, IOException {

        // Extract the Authorization header from the request
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // Check if the header contains the 'Bearer ' prefix
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            // Extract the JWT token from the header (substringing the "Bearer " part)
            jwt = authorizationHeader.substring(7);
            // Extract the username from the JWT token
            username = jwtUtil.extractUsername(jwt);
            log.debug("Extracted JWT token for username: {}", username);
        }

        // If the username is present and no authentication is set yet in the SecurityContext
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Load user details from the CustomUserDetailsService
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // Validate the token against the username
            if (Boolean.TRUE.equals(jwtUtil.validateToken(jwt, userDetails.getUsername()))) {
                log.debug("JWT is valid for user: {}", username);

                // If the token is valid, create an authentication token
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                // Set the details (request information) for the authentication token
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set the authentication token in the SecurityContext to mark the user as authenticated
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.info("User {} authenticated successfully", username);
            } else {
                log.warn("JWT token is invalid for user: {}", username);
            }
        }

        // Proceed with the filter chain to continue the request processing
        chain.doFilter(request, response);
    }
}