package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; // List import etmeyi unutma
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    // --- YENİ EKLENEN METOD ---
    // Şirket ID'sine göre kullanıcıları getir
    List<User> findAllByCompanyId(Long companyId);
}