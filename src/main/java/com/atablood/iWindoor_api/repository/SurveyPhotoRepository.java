package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.SurveyPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurveyPhotoRepository extends JpaRepository<SurveyPhoto, Long> {

    long countBySurveyItem_Id(Long surveyItemId);

    Optional<SurveyPhoto> findBySurveyItem_IdAndClientId(Long surveyItemId, String clientId);
}
