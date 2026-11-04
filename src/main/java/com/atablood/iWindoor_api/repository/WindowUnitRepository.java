package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.WindowNode;
import com.atablood.iWindoor_api.entity.WindowUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WindowUnitRepository extends JpaRepository<WindowUnit, Long> {
    List<WindowUnit> findByProjectId(Long projectId);

    Optional<WindowUnit> findByRootNode(WindowNode rootNode);
}
