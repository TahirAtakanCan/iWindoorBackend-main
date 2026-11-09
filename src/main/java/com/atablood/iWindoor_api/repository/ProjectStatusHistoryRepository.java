package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.ProjectStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectStatusHistoryRepository extends JpaRepository<ProjectStatusHistory, Long> {

    List<ProjectStatusHistory> findByProject_IdOrderByChangedAtAscIdAsc(Long projectId);
}
