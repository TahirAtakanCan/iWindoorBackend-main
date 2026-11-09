package com.atablood.iWindoor_api.repository;

import com.atablood.iWindoor_api.entity.SurveyPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyPhotoRepository extends JpaRepository<SurveyPhoto, Long> {

    long countBySurveyItem_Id(Long surveyItemId);
}
