package com.atablood.iWindoor_api.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {
    @NotBlank(message = "E-posta gerekli.")
    private String email;

    @NotBlank(message = "Şifre gerekli.")
    private String password;
}
