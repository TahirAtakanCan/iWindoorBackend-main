package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.WindowUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WindowUnitRepository extends JpaRepository<WindowUnit, Long> {
    // Bir projeye ait tüm pencereleri getir
    List<WindowUnit> findByProjectId(Long projectId);
}
