package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.Project;
import com.atablood.iWindoor_api.entity.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByCreatedBy_Company_Id(Long companyId);

    List<Project> findAllByCreatedBy_Company_IdAndStatus(Long companyId, ProjectStatus status);

    List<Project> findAllByCreatedBy_Company_IdAndCustomer_Id(Long companyId, Long customerId);

    List<Project> findAllByCreatedBy_Company_IdAndStatusAndCustomer_Id(
            Long companyId, ProjectStatus status, Long customerId);

    boolean existsByCustomer_Id(Long customerId);

    List<Project> findAllByCustomer_IdOrderByIdDesc(Long customerId);

    @Query("""
            SELECT p.status, COUNT(p) FROM Project p
            WHERE p.createdBy.company.id = :companyId
            GROUP BY p.status
            """)
    List<Object[]> countByStatus(@Param("companyId") Long companyId);

    @Query("""
            SELECT COALESCE(SUM(p.approvedTotal), 0) FROM Project p
            WHERE p.createdBy.company.id = :companyId
              AND p.approvedAt >= :from
              AND p.approvedAt < :to
            """)
    java.math.BigDecimal sumApprovedBetween(@Param("companyId") Long companyId,
                                            @Param("from") Instant from,
                                            @Param("to") Instant to);

    @Query("""
            SELECT p FROM Project p
            WHERE p.createdBy.company.id = :companyId
              AND p.deliveryDate IS NOT NULL
              AND p.deliveryDate >= :from
              AND p.deliveryDate <= :to
              AND p.status <> com.atablood.iWindoor_api.entity.ProjectStatus.DELIVERED
              AND p.status <> com.atablood.iWindoor_api.entity.ProjectStatus.CANCELLED
            ORDER BY p.deliveryDate ASC, p.id ASC
            """)
    List<Project> upcomingDeliveries(@Param("companyId") Long companyId,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    @Query("""
            SELECT p FROM Project p
            WHERE p.createdBy.company.id = :companyId
              AND p.deliveryDate IS NOT NULL
              AND p.deliveryDate < :today
              AND p.status <> com.atablood.iWindoor_api.entity.ProjectStatus.DELIVERED
              AND p.status <> com.atablood.iWindoor_api.entity.ProjectStatus.CANCELLED
            ORDER BY p.deliveryDate ASC, p.id ASC
            """)
    List<Project> overdueDeliveries(@Param("companyId") Long companyId, @Param("today") LocalDate today);

    @Query("""
            SELECT p FROM Project p
            WHERE p.createdBy.company.id = :companyId
              AND p.status IN :statuses
            """)
    List<Project> findByCompanyAndStatuses(@Param("companyId") Long companyId,
                                           @Param("statuses") List<ProjectStatus> statuses);
}
