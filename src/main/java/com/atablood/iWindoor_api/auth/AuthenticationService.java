package com.atablood.iWindoor_api.auth;

import com.atablood.iWindoor_api.entity.Company; // Eklendi
import com.atablood.iWindoor_api.entity.Role;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.repository.CompanyRepository; // Eklendi
import com.atablood.iWindoor_api.repository.UserRepository;
import com.atablood.iWindoor_api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final CompanyRepository companyRepository; // Eklendi
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        // 1. Önce otomatik bir Şirket oluştur
        Company newCompany = new Company();
        newCompany.setName(request.getFullName() + " Şirketi"); // Örn: Ahmet Yılmaz Şirketi
        newCompany = companyRepository.save(newCompany);

        // 2. Kullanıcıyı oluştur ve Şirkete bağla
        var user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.COMPANY_ADMIN)
                .company(newCompany) // <-- KRİTİK NOKTA: Kullanıcıyı şirkete bağladık
                .build();

        repository.save(user);

        var jwtToken = jwtUtil.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();
        var jwtToken = jwtUtil.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }
}