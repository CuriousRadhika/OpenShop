package com.openshop.userservice.config;

import com.openshop.userservice.model.Role;
import com.openshop.userservice.model.User;
import com.openshop.userservice.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {




    @Bean
    public CommandLineRunner initData(UserService userService) {
        return args -> {
            // Create Users
            createUserIfNotExists(
                    "customer1",
                    "Test Customer",
                    "testcustomer@abc.com",
                    "pwd",
                    Role.CUSTOMER,
                    userService
            );

            createUserIfNotExists(
                    "seller1",
                    "Test Seller",
                    "testseller@abc.com",
                    "pwd",
                    Role.SELLER,
                    userService
            );
        };

    }
    private void createUserIfNotExists (String username, String name, String email, String password, Role
            role, UserService userService){
        if (userService.findByUsername(username).isEmpty()) {
            User user = User.builder()
                    .username(username)
                    .name(name)
                    .email(email)
                    .password(password)
                    .role(role)
                    .isActive(true)
                    .build();

            System.out.println(role.name() + " user created: " + username);
            userService.createUser(user);
        } else {
            System.out.println(role.name() + " user already exists: " + username);
        }
    }
}