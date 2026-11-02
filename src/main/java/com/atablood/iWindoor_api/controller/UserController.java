package com.atablood.iWindoor_api.controller;

import com.atablood.iWindoor_api.auth.RegisterRequest;
import com.atablood.iWindoor_api.dto.UserDTO;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Şirketimdeki çalışanları listele
    // @AuthenticationPrincipal sayesinde token'dan kimin giriş yaptığını (currentUser) direkt alıyoruz.
    @GetMapping
    public ResponseEntity<List<UserDTO>> getMyEmployees(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getMyEmployees(currentUser));
    }

    // Yeni çalışan ekle
    @PreAuthorize("hasAuthority('COMPANY_ADMIN')")
    @PostMapping
    public ResponseEntity<Void> createEmployee(
            @AuthenticationPrincipal User currentUser,
            @RequestBody RegisterRequest request
    ) {
        userService.createEmployee(currentUser, request);
        return ResponseEntity.ok().build();
    }
}