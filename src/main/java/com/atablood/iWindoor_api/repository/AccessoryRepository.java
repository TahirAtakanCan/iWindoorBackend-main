package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.Accessory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessoryRepository extends JpaRepository<Accessory, Long> {

    List<Accessory> findByCompanyIdOrderByIdAsc(Long companyId);

    List<Accessory> findByCompanyIdAndActiveTrueOrderByIdAsc(Long companyId);

    List<Accessory> findByCompanyIdIsNullOrderByIdAsc();

    Optional<Accessory> findFirstByCompanyIdAndCode(Long companyId, String code);

    boolean existsByCompanyIdIsNullAndCode(String code);

    boolean existsByCompanyIdAndCode(Long companyId, String code);
}
