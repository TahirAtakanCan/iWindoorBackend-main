package com.atablood.iWindoor_api.service.impl;

import com.atablood.iWindoor_api.auth.RegisterRequest;
import com.atablood.iWindoor_api.dto.UserDTO;
import com.atablood.iWindoor_api.entity.Role;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.repository.UserRepository;
import com.atablood.iWindoor_api.service.UserService;
import com.atablood.iWindoor_api.exception.BadRequestException;
import com.atablood.iWindoor_api.exception.ConflictException;
import com.atablood.iWindoor_api.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public List<UserDTO> getMyEmployees(User currentUser) {
        // Şirket kontrolü
        if (currentUser.getCompany() == null) {
            throw new ForbiddenException("Bir şirkete bağlı değilsiniz.");
        }

        // ESKİ HATALI YÖNTEM:
        // return currentUser.getCompany().getUsers()... (Burada hata veriyordu)

        // YENİ GÜVENLİ YÖNTEM:
        // Direkt veritabanından şirkete ait kullanıcıları sorguluyoruz
        Long companyId = currentUser.getCompany().getId();
        List<User> employees = userRepository.findAllByCompanyId(companyId);

        return employees.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void createEmployee(User currentUser, RegisterRequest request) {
        if (currentUser.getCompany() == null) {
            throw new ForbiddenException("Çalışan eklemek için bir şirketiniz olmalı.");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BadRequestException("VALIDATION_FAILED", "E-posta gerekli, şifre en az 6 karakter olmalı.");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException("EMAIL_TAKEN", "Bu e-posta ile kayıtlı bir kullanıcı var.");
        }

        User employee = new User();
        employee.setFullName(request.getFullName());
        employee.setEmail(email);
        employee.setPassword(passwordEncoder.encode(request.getPassword()));
        employee.setCompany(currentUser.getCompany());
        employee.setRole(Role.EMPLOYEE);

        userRepository.save(employee);
    }

    private UserDTO mapToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        return dto;
    }
}