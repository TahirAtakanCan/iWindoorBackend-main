package com.atablood.iWindoor_api.service.impl;

import com.atablood.iWindoor_api.dto.CostItemDTO;
import com.atablood.iWindoor_api.dto.ProjectCostSummaryDTO;
import com.atablood.iWindoor_api.dto.window.PricedBom;
import com.atablood.iWindoor_api.dto.window.QuoteLine;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.pricing.BomPricer;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import com.atablood.iWindoor_api.service.PricingService;
import com.atablood.iWindoor_api.service.SalesPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * İki tür pencere var:
 *  - Yeni: bomSnapshot dolu. Birim fiyat kesim listesinden (BomPricer) gelir.
 *  - Eski: sadece window_nodes ağacı. Kaba hesap (çevre x profil fiyatı + alan x cam fiyatı).
 */
@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    private final ProjectRepository projectRepository;
    private final BomPricer bomPricer;
    private final SalesPricingService salesPricingService;

    @Override
    @Transactional
    public void calculateProjectPrice(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Proje", projectId));
        BigDecimal total = BigDecimal.ZERO;
        for (WindowUnit unit : project.getWindowUnits()) {
            if (unit.getBomSnapshot() == null) {
                double legacy = unit.getRootNode() == null ? 0.0 : legacyNodeCost(unit.getRootNode());
                unit.setUnitPrice(BigDecimal.valueOf(legacy).setScale(2, RoundingMode.HALF_UP));
            }
            BigDecimal unitPrice = unit.getUnitPrice() == null ? BigDecimal.ZERO : unit.getUnitPrice();
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(qty(unit))));
        }
        project.setTotalPrice(total.setScale(2, RoundingMode.HALF_UP));
        projectRepository.save(project);
        salesPricingService.recalculateProjectSales(projectId);
    }

    @Override
    @Transactional
    public void syncPrices(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Proje", projectId));
        Long companyId = companyOf(project);
        for (WindowUnit unit : project.getWindowUnits()) {
            if (unit.getBomSnapshot() != null) {
                unit.setUnitPrice(bomPricer.price(companyId, unit.getSeries(), unit.getBomSnapshot()).total());
            } else if (unit.getRootNode() != null) {
                refreshStoredPrices(unit.getRootNode());
            }
        }
        projectRepository.save(project);
        calculateProjectPrice(projectId);
    }

    @Override
    public ProjectCostSummaryDTO calculateCostSummary(Project project) {
        ProjectCostSummaryDTO summary = new ProjectCostSummaryDTO();
        Map<String, CostItemDTO> items = new LinkedHashMap<>();
        Long companyId = companyOf(project);

        for (WindowUnit unit : project.getWindowUnits()) {
            int q = qty(unit);
            if (unit.getBomSnapshot() != null) {
                PricedBom priced = bomPricer.price(companyId, unit.getSeries(), unit.getBomSnapshot());
                for (QuoteLine l : priced.lines()) {
                    merge(items, l.name(), category(l.group()), l.quantity() * q, l.unit(),
                            l.total().multiply(BigDecimal.valueOf(q)));
                }
            } else if (unit.getRootNode() != null) {
                collectLegacy(unit.getRootNode(), items, q);
            }
        }
        items.values().forEach(i -> {
            i.setQuantity(Math.round(i.getQuantity() * 1000.0) / 1000.0);
            i.setPrice(i.getPrice().setScale(2, RoundingMode.HALF_UP));
            summary.addItem(i);
        });
        return summary;
    }

    // ---------------- yardımcılar ----------------

    private static int qty(WindowUnit u) {
        return u.getQuantity() == null || u.getQuantity() < 1 ? 1 : u.getQuantity();
    }

    private static Long companyOf(Project p) {
        return p.getCreatedBy() == null || p.getCreatedBy().getCompany() == null ? null : p.getCreatedBy().getCompany().getId();
    }

    private static String category(String group) {
        return switch (group) {
            case "PROFILE" -> "Profil";
            case "BEAD" -> "Çıta";
            case "DUPLEX" -> "Dubleks";
            case "GLASS" -> "Cam";
            case "PANEL" -> "Panel";
            case "ACCESSORY" -> "Aksesuar";
            case "COLOR" -> "Renk farkı";
            default -> "Diğer";
        };
    }

    private static void merge(Map<String, CostItemDTO> items, String name, String category, double quantity, String unit,
                              BigDecimal price) {
        items.merge(category + "|" + name, new CostItemDTO(name, category, quantity, unit, price), (a, b) -> {
            a.setQuantity(a.getQuantity() + b.getQuantity());
            a.setPrice(a.getPrice().add(b.getPrice()));
            return a;
        });
    }

    private static double legacyLengthM(WindowNode node) {
        double w = node.getWidth() == null ? 0 : node.getWidth() / 1000.0;
        double h = node.getHeight() == null ? 0 : node.getHeight() / 1000.0;
        return switch (node.getNodeType() == null ? NodeType.EMPTY : node.getNodeType()) {
            case FRAME, SASH -> (w + h) * 2;
            case MULLION_VERTICAL -> h;
            case MULLION_HORIZONTAL -> w;
            default -> 0.0;
        };
    }

    private double legacyNodeCost(WindowNode node) {
        double cost = 0.0;
        if (node.getProfile() != null) {
            BigDecimal p = node.getStoredPrice() != null ? node.getStoredPrice() : node.getProfile().getPricePerMeter();
            cost += legacyLengthM(node) * p.doubleValue();
        }
        if (node.getGlass() != null) {
            BigDecimal p = node.getStoredPrice() != null ? node.getStoredPrice() : node.getGlass().getPricePerSquareMeter();
            cost += area(node) * p.doubleValue();
        }
        if (node.getChildren() != null) for (WindowNode c : node.getChildren()) cost += legacyNodeCost(c);
        return cost;
    }

    private static double area(WindowNode n) {
        return (n.getWidth() == null ? 0 : n.getWidth()) * (n.getHeight() == null ? 0 : n.getHeight()) / 1_000_000.0;
    }

    private void refreshStoredPrices(WindowNode node) {
        if (node.getProfile() != null) node.setStoredPrice(node.getProfile().getPricePerMeter());
        else if (node.getGlass() != null) node.setStoredPrice(node.getGlass().getPricePerSquareMeter());
        if (node.getChildren() != null) node.getChildren().forEach(this::refreshStoredPrices);
    }

    private void collectLegacy(WindowNode node, Map<String, CostItemDTO> items, int q) {
        if (node.getProfile() != null) {
            double m = legacyLengthM(node);
            if (m > 0) {
                BigDecimal p = node.getStoredPrice() != null ? node.getStoredPrice() : node.getProfile().getPricePerMeter();
                merge(items, node.getProfile().getName(), "Profil", m * q, "m", p.multiply(BigDecimal.valueOf(m * q)));
            }
        }
        if (node.getGlass() != null) {
            double a = area(node);
            BigDecimal p = node.getStoredPrice() != null ? node.getStoredPrice() : node.getGlass().getPricePerSquareMeter();
            merge(items, node.getGlass().getName(), "Cam", a * q, "m2", p.multiply(BigDecimal.valueOf(a * q)));
        }
        if (node.getChildren() != null) for (WindowNode c : node.getChildren()) collectLegacy(c, items, q);
    }
}
