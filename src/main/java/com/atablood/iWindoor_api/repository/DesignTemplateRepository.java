package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.DesignTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignTemplateRepository extends JpaRepository<DesignTemplate, Long> {

    List<DesignTemplate> findByCompanyIdOrderBySortOrderAscNameAsc(Long companyId);

    List<DesignTemplate> findByCompanyIdAndCategoryOrderBySortOrderAscNameAsc(Long companyId, String category);

    long countByCompanyId(Long companyId);

    boolean existsByCompanyIdAndNameIgnoreCase(Long companyId, String name);

    boolean existsByCompanyIdAndNameIgnoreCaseAndIdNot(Long companyId, String name, Long id);

    Optional<DesignTemplate> findByIdAndCompanyId(Long id, Long companyId);
}
