package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeriesRepository extends JpaRepository<Series, Long> {
    boolean existsByName(String name);

    boolean existsByCompanyId(Long companyId);

    boolean existsByCompanyIdIsNullAndName(String name);

    List<Series> findByCompanyIdOrderByIdAsc(Long companyId);

    List<Series> findByCompanyIdIsNullOrderByIdAsc();

    Optional<Series> findFirstByCompanyIdAndActiveTrueOrderByIdAsc(Long companyId);

    Optional<Series> findFirstByCompanyIdAndName(Long companyId, String name);
}
