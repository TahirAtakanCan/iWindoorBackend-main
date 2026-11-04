package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.dto.ProjectCostSummaryDTO;
import com.atablood.iWindoor_api.entity.Project;

public interface PricingService {
    /** Pencerelerin birim fiyatlarını ve proje toplamını günceller (adet dahil). */
    void calculateProjectPrice(Long projectId);

    /** Projenin malzeme bazlı maliyet dökümü. */
    ProjectCostSummaryDTO calculateCostSummary(Project project);

    /** Dondurulmuş fiyatları güncel katalog fiyatlarıyla yeniler, sonra toplamı hesaplar. */
    void syncPrices(Long projectId);
}
