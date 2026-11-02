package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.dto.ProjectCostSummaryDTO;
import com.atablood.iWindoor_api.entity.Project;

public interface PricingService {
    // Veritabanına kaydeder (ID ile çalışır)
    void calculateProjectPrice(Long projectId);

    // Rapor için hesaplar (Nesne ile çalışır - Daha hızlı)
    ProjectCostSummaryDTO calculateCostSummary(Project project);
}