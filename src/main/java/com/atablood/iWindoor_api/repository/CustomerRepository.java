package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByCompanyIdOrderByNameAscIdAsc(Long companyId);

    @Query("""
            SELECT c FROM Customer c
            WHERE c.companyId = :companyId
              AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR (c.phone IS NOT NULL AND c.phone LIKE CONCAT('%', :q, '%')))
            ORDER BY c.name ASC, c.id ASC
            """)
    List<Customer> search(@Param("companyId") Long companyId, @Param("q") String q);

    @Query("""
            SELECT c FROM Customer c
            WHERE c.companyId = :companyId
              AND LOWER(c.name) = LOWER(:name)
              AND COALESCE(c.phone, '') = COALESCE(:phone, '')
            """)
    Optional<Customer> findMatch(@Param("companyId") Long companyId,
                                 @Param("name") String name,
                                 @Param("phone") String phone);
}
