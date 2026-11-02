package com.atablood.iWindoor_api.service.impl;

import com.atablood.iWindoor_api.auth.RegisterRequest;
import com.atablood.iWindoor_api.dto.UserDTO;
import com.atablood.iWindoor_api.entity.Role;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.repository.UserRepository;
import com.atablood.iWindoor_api.service.UserService;
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
            throw new RuntimeException("Bir şirkete bağlı değilsiniz.");
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
            throw new RuntimeException("Çalışan eklemek için bir şirketiniz olmalı.");
        }

        // Yeni çalışanı oluştur
        User employee = new User();
        employee.setFullName(request.getFullName());
        employee.setEmail(request.getEmail());
        employee.setPassword(passwordEncoder.encode(request.getPassword()));

        // ÖNEMLİ: Ekleyen kişinin şirketi neyse, çalışanınki de o olur.
        employee.setCompany(currentUser.getCompany());

        // Rolü otomatik olarak 'EMPLOYEE' (Çalışan) yapıyoruz
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