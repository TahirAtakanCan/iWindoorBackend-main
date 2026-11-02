package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.Glass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlassRepository extends JpaRepository<Glass, Long> {
    // Aktif olan camları listele (Pasifleri getirme)
    // List<Glass> findByIsActiveTrue(); // Glass entity'sine isActive eklediysen bunu açabilirsin
}