package com.atablood.iWindoor_api.dto.customer;

import com.atablood.iWindoor_api.entity.Customer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CustomerDto(
        Long id,
        String name,
        String phone,
        String email,
        String address,
        String taxOffice,
        String taxNumber,
        String note,
        Instant createdAt,
        List<CustomerProjectLine> projects,
        BigDecimal totalBalance
) {
    public static CustomerDto of(Customer c) {
        return new CustomerDto(c.getId(), c.getName(), c.getPhone(), c.getEmail(), c.getAddress(),
                c.getTaxOffice(), c.getTaxNumber(), c.getNote(), c.getCreatedAt(), null, null);
    }

    public static CustomerDto of(Customer c, List<CustomerProjectLine> projects, BigDecimal totalBalance) {
        return new CustomerDto(c.getId(), c.getName(), c.getPhone(), c.getEmail(), c.getAddress(),
                c.getTaxOffice(), c.getTaxNumber(), c.getNote(), c.getCreatedAt(), projects, totalBalance);
    }

    public record Request(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 50) String phone,
            @Email @Size(max = 200) String email,
            @Size(max = 500) String address,
            @Size(max = 120) String taxOffice,
            @Size(max = 50) String taxNumber,
            @Size(max = 2000) String note
    ) {
    }

    public record CustomerProjectLine(
            Long id,
            String name,
            String status,
            BigDecimal target,
            BigDecimal paid,
            BigDecimal balance
    ) {
    }
}
