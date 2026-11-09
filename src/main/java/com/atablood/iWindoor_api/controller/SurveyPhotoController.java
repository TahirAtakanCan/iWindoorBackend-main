package com.atablood.iWindoor_api.controller;

import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/survey-photos")
@RequiredArgsConstructor
public class SurveyPhotoController {

    private final SurveyService surveyService;

    @GetMapping("/{photoId}")
    public ResponseEntity<byte[]> get(@PathVariable Long photoId, @AuthenticationPrincipal User user) {
        return surveyService.getPhoto(photoId, user);
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> delete(@PathVariable Long photoId, @AuthenticationPrincipal User user) {
        surveyService.deletePhoto(photoId, user);
        return ResponseEntity.noContent().build();
    }
}
