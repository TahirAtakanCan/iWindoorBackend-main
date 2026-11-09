package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByProject_IdOrderByPaidAtAscIdAsc(Long projectId);

    Optional<Payment> findByIdAndProject_Id(Long id, Long projectId);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN p.kind = 'PAYMENT' THEN p.amount ELSE -p.amount END), 0)
            FROM Payment p WHERE p.project.id = :projectId
            """)
    BigDecimal paidNet(@Param("projectId") Long projectId);
}
