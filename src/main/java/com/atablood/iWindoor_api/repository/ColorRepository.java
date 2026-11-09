package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ColorRepository extends JpaRepository<Color, Long> {

    List<Color> findByCompanyIdOrderBySortOrderAscIdAsc(Long companyId);

    List<Color> findByCompanyIdAndActiveTrueOrderBySortOrderAscIdAsc(Long companyId);

    List<Color> findByCompanyIdIsNullOrderBySortOrderAscIdAsc();

    Optional<Color> findFirstByCompanyIdAndCode(Long companyId, String code);

    boolean existsByCompanyIdAndCode(Long companyId, String code);

    boolean existsByCompanyIdIsNullAndCode(String code);
}
