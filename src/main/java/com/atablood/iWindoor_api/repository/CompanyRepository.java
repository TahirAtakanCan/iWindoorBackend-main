package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.Company;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    /** Şirket satırını kilitler: aynı şirket için katalog iki kez kopyalanmasın. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Company c where c.id = :id")
    Optional<Company> lockById(@Param("id") Long id);
}
