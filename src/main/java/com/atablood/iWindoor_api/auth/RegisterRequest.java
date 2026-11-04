package com.atablood.iWindoor_api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Ad soyad gerekli.")
    @Size(max = 120, message = "Ad soyad en fazla 120 karakter olabilir.")
    private String fullName;

    @NotBlank(message = "E-posta gerekli.")
    @Email(message = "Geçerli bir e-posta girin.")
    @Size(max = 200, message = "E-posta en fazla 200 karakter olabilir.")
    private String email;

    @NotBlank(message = "Şifre gerekli.")
    @Size(min = 6, max = 100, message = "Şifre en az 6, en fazla 100 karakter olmalı.")
    private String password;

    /** Opsiyonel; boşsa "<fullName> Şirketi" kullanılır. */
    @Size(max = 200, message = "Firma adı en fazla 200 karakter olabilir.")
    private String companyName;
}
