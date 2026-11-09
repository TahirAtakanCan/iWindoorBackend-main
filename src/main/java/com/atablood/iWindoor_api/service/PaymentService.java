package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.dto.project.PaymentDto;
import com.atablood.iWindoor_api.dto.project.PaymentDto.ListResponse;
import com.atablood.iWindoor_api.dto.project.PaymentRequest;
import com.atablood.iWindoor_api.entity.Payment;
import com.atablood.iWindoor_api.entity.Project;
import com.atablood.iWindoor_api.entity.Role;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.exception.BadRequestException;
import com.atablood.iWindoor_api.exception.ForbiddenException;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.repository.PaymentRepository;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectFinanceService financeService;
    private final AccessGuard guard;

    @Transactional(readOnly = true)
    public ListResponse list(Long projectId, User user) {
        Project project = loadProject(projectId, user);
        List<PaymentDto> payments = paymentRepository.findByProject_IdOrderByPaidAtAscIdAsc(projectId).stream()
                .map(PaymentDto::of)
                .toList();
        BigDecimal paid = financeService.paidNet(projectId);
        BigDecimal target = financeService.target(project, user);
        BigDecimal balance = target.subtract(paid).setScale(2, RoundingMode.HALF_UP);
        return new ListResponse(payments, paid, target, balance);
    }

    @Transactional
    public PaymentDto create(Long projectId, PaymentRequest req, User user) {
        Project project = loadProject(projectId, user);
        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("VALIDATION_FAILED", "Tutar 0'dan büyük olmalı.");
        }
        Payment p = new Payment();
        p.setProject(project);
        p.setKind(req.kind());
        p.setAmount(req.amount().setScale(2, RoundingMode.HALF_UP));
        p.setPaidAt(req.paidAt());
        p.setMethod(req.method());
        p.setNote(req.note());
        p.setCreatedBy(user);
        return PaymentDto.of(paymentRepository.save(p));
    }

    @Transactional
    public void delete(Long projectId, Long paymentId, User user) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.COMPANY_ADMIN) {
            throw new ForbiddenException("Ödeme silme yalnızca yönetici tarafından yapılabilir.");
        }
        loadProject(projectId, user);
        Payment p = paymentRepository.findByIdAndProject_Id(paymentId, projectId)
                .orElseThrow(() -> new NotFoundException("Ödeme", paymentId));
        paymentRepository.delete(p);
    }

    private Project loadProject(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Proje", projectId));
        guard.checkProject(project, user);
        return project;
    }
}
