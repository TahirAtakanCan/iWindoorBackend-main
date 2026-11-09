package com.atablood.iWindoor_api.dto.project;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @Size(max = 200) String customerName,
        @Size(max = 50) String customerPhone,
        @Email @Size(max = 200) String customerEmail,
        @Size(max = 500) String customerAddress,
        Long customerId
) {
}
