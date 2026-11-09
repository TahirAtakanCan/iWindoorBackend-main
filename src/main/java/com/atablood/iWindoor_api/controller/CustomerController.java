package com.atablood.iWindoor_api.controller;

import com.atablood.iWindoor_api.dto.customer.CustomerDto;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public List<CustomerDto> list(@RequestParam(required = false) String q,
                                  @AuthenticationPrincipal User user) {
        return customerService.list(q, user);
    }

    @PostMapping
    public CustomerDto create(@Valid @RequestBody CustomerDto.Request request,
                              @AuthenticationPrincipal User user) {
        return customerService.create(request, user);
    }

    @GetMapping("/{id}")
    public CustomerDto get(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return customerService.get(id, user);
    }

    @PutMapping("/{id}")
    public CustomerDto update(@PathVariable Long id, @Valid @RequestBody CustomerDto.Request request,
                              @AuthenticationPrincipal User user) {
        return customerService.update(id, request, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        customerService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
