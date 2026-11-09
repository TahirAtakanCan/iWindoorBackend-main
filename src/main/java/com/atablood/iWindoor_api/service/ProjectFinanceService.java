package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.entity.Project;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ProjectFinanceService {

    private final PaymentRepository paymentRepository;
    private final ProjectQuoteService projectQuoteService;

    @Transactional(readOnly = true)
    public BigDecimal currentGrandTotal(Project project, User user) {
        return projectQuoteService.quote(project.getId(), null, user).grandTotal();
    }

    @Transactional(readOnly = true)
    public BigDecimal paidNet(Long projectId) {
        BigDecimal paid = paymentRepository.paidNet(projectId);
        return money(paid == null ? BigDecimal.ZERO : paid);
    }

    @Transactional(readOnly = true)
    public BigDecimal target(Project project, User user) {
        if (project.getApprovedTotal() != null) {
            return money(project.getApprovedTotal());
        }
        return currentGrandTotal(project, user);
    }

    @Transactional(readOnly = true)
    public void enrich(Project project, User user) {
        BigDecimal paid = paidNet(project.getId());
        BigDecimal current = currentGrandTotal(project, user);
        BigDecimal tgt = project.getApprovedTotal() != null ? money(project.getApprovedTotal()) : current;
        project.setPaidTotal(paid);
        project.setBalance(money(tgt.subtract(paid)));
        if (project.getApprovedTotal() != null) {
            boolean changed = current.compareTo(money(project.getApprovedTotal())) != 0;
            project.setPriceChangedAfterApproval(changed);
            project.setPriceDelta(changed ? money(current.subtract(project.getApprovedTotal())) : BigDecimal.ZERO.setScale(2));
        } else {
            project.setPriceChangedAfterApproval(false);
            project.setPriceDelta(null);
        }
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
