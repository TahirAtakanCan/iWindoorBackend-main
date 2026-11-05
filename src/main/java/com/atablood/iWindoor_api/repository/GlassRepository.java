package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.Glass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlassRepository extends JpaRepository<Glass, Long> {

    List<Glass> findByCompanyIdOrderByIdAsc(Long companyId);

    List<Glass> findByCompanyIdAndIsActiveTrueOrderByIdAsc(Long companyId);

    List<Glass> findByCompanyIdIsNullOrderByIdAsc();

    Optional<Glass> findFirstByCompanyIdAndCode(Long companyId, String code);

    boolean existsByCompanyIdAndCode(Long companyId, String code);

    boolean existsByCompanyIdIsNullAndCode(String code);
}
