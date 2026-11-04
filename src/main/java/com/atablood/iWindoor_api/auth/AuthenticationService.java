package com.atablood.iWindoor_api.auth;

import com.atablood.iWindoor_api.entity.Company;
import com.atablood.iWindoor_api.entity.Role;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.exception.ConflictException;
import com.atablood.iWindoor_api.repository.CompanyRepository;
import com.atablood.iWindoor_api.repository.UserRepository;
import com.atablood.iWindoor_api.security.JwtUtil;
import com.atablood.iWindoor_api.service.CatalogProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CatalogProvisioningService catalogProvisioning;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (repository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException("EMAIL_TAKEN", "Bu e-posta ile kayıtlı bir kullanıcı var.");
        }

        String companyName = request.getCompanyName();
        if (companyName == null || companyName.isBlank()) {
            companyName = request.getFullName() + " Şirketi";
        } else {
            companyName = companyName.trim();
        }

        Company newCompany = new Company();
        newCompany.setName(companyName);
        newCompany = companyRepository.save(newCompany);

        var user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.COMPANY_ADMIN)
                .company(newCompany)
                .build();

        repository.save(user);
        catalogProvisioning.ensureCompanyCatalog(newCompany.getId());

        var jwtToken = jwtUtil.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        String email = normalizeEmail(request.getEmail());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );
        var user = repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Kullanıcı bulunamadı"));
        var jwtToken = jwtUtil.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
