package com.atablood.iWindoor_api.dto.quote;

import com.atablood.iWindoor_api.entity.Project;
import com.atablood.iWindoor_api.entity.QuoteVersion;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record QuoteVersionDto(
        Long id,
        Integer versionNo,
        String label,
        BigDecimal grandTotal,
        BigDecimal saleSubtotal,
        Integer windowCount,
        Instant createdAt,
        String createdByName,
        boolean approved
) {
    public static QuoteVersionDto of(QuoteVersion v, Project project) {
        Long approvedId = project.getApprovedVersion() == null ? null : project.getApprovedVersion().getId();
        String by = v.getCreatedBy() == null ? null : v.getCreatedBy().getFullName();
        return new QuoteVersionDto(v.getId(), v.getVersionNo(), v.getLabel(), v.getGrandTotal(),
                v.getSaleSubtotal(), v.getWindowCount(), v.getCreatedAt(), by,
                approvedId != null && approvedId.equals(v.getId()));
    }

    public record CreateRequest(@Size(max = 200) String label) {
    }

    public record DetailDto(Long id, Integer versionNo, String label, Map<String, Object> snapshot,
                            BigDecimal grandTotal, BigDecimal saleSubtotal, Integer windowCount,
                            Instant createdAt, String createdByName, boolean approved) {
        public static DetailDto of(QuoteVersion v, Project project) {
            QuoteVersionDto base = QuoteVersionDto.of(v, project);
            return new DetailDto(base.id(), base.versionNo(), base.label(), v.getSnapshot(),
                    base.grandTotal(), base.saleSubtotal(), base.windowCount(),
                    base.createdAt(), base.createdByName(), base.approved());
        }
    }

    public record RestoreResponse(Project project, List<String> warnings) {
    }
}
