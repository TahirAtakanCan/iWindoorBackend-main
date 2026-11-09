package com.atablood.iWindoor_api.dto.project;

import com.atablood.iWindoor_api.entity.ProjectStatus;
import com.atablood.iWindoor_api.entity.ProjectStatusHistory;

import java.time.Instant;

public record StatusHistoryDto(
        Long id,
        ProjectStatus fromStatus,
        ProjectStatus toStatus,
        String note,
        Long changedById,
        Instant changedAt
) {
    public static StatusHistoryDto of(ProjectStatusHistory h) {
        return new StatusHistoryDto(h.getId(), h.getFromStatus(), h.getToStatus(), h.getNote(),
                h.getChangedById(), h.getChangedAt());
    }
}
