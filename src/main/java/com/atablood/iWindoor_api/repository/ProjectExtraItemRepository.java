package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.ProjectExtraItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectExtraItemRepository extends JpaRepository<ProjectExtraItem, Long> {

    List<ProjectExtraItem> findByProject_IdOrderBySortOrderAscIdAsc(Long projectId);

    Optional<ProjectExtraItem> findByIdAndProject_Id(Long id, Long projectId);

    void deleteByProject_Id(Long projectId);
}
