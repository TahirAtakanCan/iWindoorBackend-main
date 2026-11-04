package com.atablood.iWindoor_api.dto.window;

import com.atablood.iWindoor_api.design.DesignDocuments;
import com.atablood.iWindoor_api.entity.WindowUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Pencere + tasarım belgesi. legacyDesign=true ise belge eski düğüm ağacından türetilmiştir;
 * ilk PUT ile kalıcı belgeye dönüşür.
 */
public record WindowDto(
        Long id,
        Long projectId,
        String name,
        Double width,
        Double height,
        Integer quantity,
        Long seriesId,
        Map<String, Object> design,
        boolean legacyDesign,
        List<Map<String, Object>> bom,
        BigDecimal unitPrice,
        Long rowVersion,
        LocalDateTime updatedAt
) {
    public static WindowDto of(WindowUnit u) {
        boolean legacy = u.getDesign() == null;
        return new WindowDto(u.getId(), u.getProject() == null ? null : u.getProject().getId(), u.getName(),
                u.getWidth(), u.getHeight(), u.getQuantity(), u.getSeriesId(),
                legacy ? DesignDocuments.fromLegacy(u) : u.getDesign(), legacy,
                u.getBomSnapshot(), u.getUnitPrice(), u.getRowVersion(), u.getUpdatedAt());
    }
}
