package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.dto.dashboard.DashboardDto;
import com.atablood.iWindoor_api.dto.dashboard.DashboardDto.DeliveryLine;
import com.atablood.iWindoor_api.entity.Project;
import com.atablood.iWindoor_api.entity.ProjectStatus;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<ProjectStatus> RECEIVABLE_STATUSES = List.of(
            ProjectStatus.APPROVED, ProjectStatus.PRODUCTION, ProjectStatus.INSTALLATION, ProjectStatus.DELIVERED);

    private final ProjectRepository projectRepository;
    private final ProjectFinanceService financeService;
    private final AccessGuard guard;

    @Transactional(readOnly = true)
    public DashboardDto get(User user) {
        Long companyId = guard.companyId(user);

        Map<String, Long> counts = new LinkedHashMap<>();
        for (ProjectStatus s : ProjectStatus.values()) counts.put(s.name(), 0L);
        for (Object[] row : projectRepository.countByStatus(companyId)) {
            ProjectStatus status = (ProjectStatus) row[0];
            counts.put(status.name(), (Long) row[1]);
        }

        ZoneId zone = ZoneId.systemDefault();
        YearMonth ym = YearMonth.now(zone);
        Instant from = ym.atDay(1).atStartOfDay(zone).toInstant();
        Instant to = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        BigDecimal approvedThisMonth = money(projectRepository.sumApprovedBetween(companyId, from, to));

        BigDecimal receivables = BigDecimal.ZERO;
        for (Project p : projectRepository.findByCompanyAndStatuses(companyId, RECEIVABLE_STATUSES)) {
            BigDecimal paid = financeService.paidNet(p.getId());
            BigDecimal target = financeService.target(p, user);
            BigDecimal bal = target.subtract(paid);
            if (bal.compareTo(BigDecimal.ZERO) > 0) {
                receivables = receivables.add(bal);
            }
        }
        receivables = money(receivables);

        LocalDate today = LocalDate.now(zone);
        List<DeliveryLine> upcoming = projectRepository.upcomingDeliveries(companyId, today, today.plusDays(14))
                .stream().map(this::line).toList();
        List<DeliveryLine> overdue = projectRepository.overdueDeliveries(companyId, today)
                .stream().map(this::line).toList();

        return new DashboardDto(counts, approvedThisMonth, receivables, upcoming, overdue);
    }

    private DeliveryLine line(Project p) {
        return new DeliveryLine(p.getId(), p.getName(), p.getCustomerName(), p.getDeliveryDate(), p.getStatus());
    }

    private static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }
}
