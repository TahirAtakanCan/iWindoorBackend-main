package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    // İleride "Müşteri adına göre ara" özelliği ekleyeceğiz
    // List<Project> findByNameContainingIgnoreCase(String name);
    List<Project> findAllByCreatedBy_Company_Id(Long companyId);
}
