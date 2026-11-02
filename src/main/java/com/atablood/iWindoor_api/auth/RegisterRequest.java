package com.atablood.iWindoor_api.auth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    // Şimdilik sadece Admin veya Şirket Yetkilisi kaydoluyor varsayalım
    // İleride şirket ID'si de alacağız.
}