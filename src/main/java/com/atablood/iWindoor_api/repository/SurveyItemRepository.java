package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.SurveyItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SurveyItemRepository extends JpaRepository<SurveyItem, Long> {

    List<SurveyItem> findByProject_IdOrderBySortOrderAscIdAsc(Long projectId);

    Optional<SurveyItem> findByIdAndProject_Id(Long id, Long projectId);

    Optional<SurveyItem> findByProject_IdAndClientId(Long projectId, String clientId);
}
