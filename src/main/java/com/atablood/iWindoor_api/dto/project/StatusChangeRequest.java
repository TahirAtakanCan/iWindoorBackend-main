package com.atablood.iWindoor_api.dto.project;

import com.atablood.iWindoor_api.entity.ProjectStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StatusChangeRequest(
        @NotNull ProjectStatus status,
        @Size(max = 500) String note,
        LocalDate deliveryDate
) {
}
