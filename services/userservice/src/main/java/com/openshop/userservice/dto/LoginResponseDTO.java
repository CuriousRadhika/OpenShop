package com.openshop.userservice.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class LoginResponseDTO {
    private String token;
    private Long expiresIn;
    private UserDTO user;

    public LoginResponseDTO(String token, Long expiresIn,UserDTO user) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.user = user;
    }
}
