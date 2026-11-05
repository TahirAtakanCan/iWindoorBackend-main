package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.HardwareRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HardwareRuleRepository extends JpaRepository<HardwareRule, Long> {

    List<HardwareRule> findByCompanyIdOrderBySortOrderAscIdAsc(Long companyId);

    List<HardwareRule> findByCompanyIdIsNullOrderBySortOrderAscIdAsc();

    List<HardwareRule> findByCompanyIdAndSeriesIdAndKindOrderBySortOrderAscIdAsc(Long companyId, Long seriesId, String kind);

    List<HardwareRule> findByCompanyIdAndSeriesIdIsNullAndKindOrderBySortOrderAscIdAsc(Long companyId, String kind);

    boolean existsByCompanyIdIsNullAndKindAndAccessoryCodeAndSeriesIdIsNull(String kind, String accessoryCode);

    boolean existsByCompanyIdAndKindAndAccessoryCodeAndSeriesIdIsNull(Long companyId, String kind, String accessoryCode);
}
