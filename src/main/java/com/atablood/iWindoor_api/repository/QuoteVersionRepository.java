package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.QuoteVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuoteVersionRepository extends JpaRepository<QuoteVersion, Long> {

    List<QuoteVersion> findByProject_IdOrderByVersionNoAsc(Long projectId);

    Optional<QuoteVersion> findByIdAndProject_Id(Long id, Long projectId);

    long countByProject_Id(Long projectId);

    @Query("SELECT COALESCE(MAX(v.versionNo), 0) FROM QuoteVersion v WHERE v.project.id = :projectId")
    int maxVersionNo(@Param("projectId") Long projectId);
}
