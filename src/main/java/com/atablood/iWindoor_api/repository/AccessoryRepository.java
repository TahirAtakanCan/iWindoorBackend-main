package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.Accessory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessoryRepository extends JpaRepository<Accessory, Long> {
    Accessory findByCode(String code);
}
