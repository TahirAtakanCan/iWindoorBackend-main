package com.atablood.iWindoor_api.service.impl;

import com.atablood.iWindoor_api.dto.CostItemDTO;
import com.atablood.iWindoor_api.dto.ProjectCostSummaryDTO;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import com.atablood.iWindoor_api.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    private final ProjectRepository projectRepository;

    @Override
    @Transactional
    public void calculateProjectPrice(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proje bulunamadı"));

        double totalProjectPrice = 0.0;

        for (WindowUnit unit : project.getWindowUnits()) {
            if (unit.getRootNode() != null) {
                totalProjectPrice += calculateNodeCost(unit.getRootNode());
            }
        }

        project.setTotalPrice(BigDecimal.valueOf(totalProjectPrice));
        projectRepository.save(project);
    }

    // --- MEO4 İÇİN GÜNCELLENEN METOD ---
    @Override
    public ProjectCostSummaryDTO calculateCostSummary(Project project) {
        // Not: Burada tekrar repository'den çekmeye gerek yok,
        // çünkü ProjectServiceImpl zaten güvenlik kontrolünü yapıp bize temiz nesneyi verdi.

        ProjectCostSummaryDTO summary = new ProjectCostSummaryDTO();
        Map<String, CostItemDTO> profileMap = new HashMap<>();
        Map<String, CostItemDTO> glassMap = new HashMap<>();

        if (project.getWindowUnits() != null) {
            for (WindowUnit unit : project.getWindowUnits()) {
                if (unit.getRootNode() != null) {
                    collectMaterials(unit.getRootNode(), profileMap, glassMap);
                }
            }
        }

        profileMap.values().forEach(summary::addItem);
        glassMap.values().forEach(summary::addItem);

        return summary;
    }

    // --- YARDIMCI METODLAR (Aynı kalıyor) ---

    private double calculateNodeCost(WindowNode node) {
        double nodeCost = 0.0;

        double widthM = node.getWidth() / 1000.0;
        double heightM = node.getHeight() / 1000.0;

        // 1. PROFİL MALİYETİ
        if (node.getProfile() != null) {
            double lengthM = 0.0;

            BigDecimal unitPriceBd = (node.getStoredPrice() != null)
                    ? node.getStoredPrice()
                    : node.getProfile().getPricePerMeter();

            double price = unitPriceBd.doubleValue();

            if (node.getNodeType().name().equals("FRAME") || node.getNodeType().name().equals("SASH")) {
                lengthM = (widthM + heightM) * 2;
            } else if (node.getNodeType().name().equals("MULLION_VERTICAL")) {
                lengthM = heightM;
            } else if (node.getNodeType().name().equals("MULLION_HORIZONTAL")) {
                lengthM = widthM;
            }

            double cost = lengthM * price;
            nodeCost += cost;
        }

        // 2. CAM MALİYETİ
        if (node.getGlass() != null) {
            BigDecimal unitPriceBd = (node.getStoredPrice() != null)
                    ? node.getStoredPrice()
                    : node.getGlass().getPricePerSquareMeter();

            double area = widthM * heightM;
            nodeCost += (area * unitPriceBd.doubleValue());
        }

        // 3. ÇOCUKLAR
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (WindowNode child : node.getChildren()) {
                nodeCost += calculateNodeCost(child);
            }
        }

        return nodeCost;
    }

    private void collectMaterials(WindowNode node, Map<String, CostItemDTO> profileMap, Map<String, CostItemDTO> glassMap) {
        // 1. Profil Hesabı
        if (node.getProfile() != null) {
            String name = node.getProfile().getName();
            double lengthM = 0;

            if (node.getNodeType() == NodeType.FRAME || node.getNodeType() == NodeType.SASH) {
                lengthM = (node.getWidth() * 2 + node.getHeight() * 2) / 1000.0;
            } else if (node.getNodeType() == NodeType.MULLION_VERTICAL) {
                lengthM = node.getHeight() / 1000.0;
            } else if (node.getNodeType() == NodeType.MULLION_HORIZONTAL) {
                lengthM = node.getWidth() / 1000.0;
            }

            if (lengthM > 0) {
                BigDecimal unitPrice = (node.getStoredPrice() != null)
                        ? node.getStoredPrice()
                        : node.getProfile().getPricePerMeter();

                BigDecimal cost = unitPrice.multiply(BigDecimal.valueOf(lengthM));

                profileMap.merge(name,
                        new CostItemDTO(name, "Profil", lengthM, "m", cost),
                        (oldItem, newItem) -> {
                            oldItem.setQuantity(oldItem.getQuantity() + newItem.getQuantity());
                            oldItem.setPrice(oldItem.getPrice().add(newItem.getPrice()));
                            return oldItem;
                        }
                );
            }
        }

        // 2. Cam Hesabı
        if (node.getGlass() != null) {
            String name = node.getGlass().getName();
            double area = (node.getWidth() * node.getHeight()) / 1_000_000.0;

            BigDecimal unitPrice = (node.getStoredPrice() != null)
                    ? node.getStoredPrice()
                    : node.getGlass().getPricePerSquareMeter();

            BigDecimal cost = unitPrice.multiply(BigDecimal.valueOf(area));

            glassMap.merge(name,
                    new CostItemDTO(name, "Cam", area, "m2", cost),
                    (oldItem, newItem) -> {
                        oldItem.setQuantity(oldItem.getQuantity() + newItem.getQuantity());
                        oldItem.setPrice(oldItem.getPrice().add(newItem.getPrice()));
                        return oldItem;
                    }
            );
        }

        if (node.getChildren() != null) {
            for (WindowNode child : node.getChildren()) {
                collectMaterials(child, profileMap, glassMap);
            }
        }
    }
}