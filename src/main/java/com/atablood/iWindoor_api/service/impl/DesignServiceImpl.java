package com.atablood.iWindoor_api.service.impl;

import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.exception.BadRequestException;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.repository.GlassRepository;
import com.atablood.iWindoor_api.repository.ProfileRepository;
import com.atablood.iWindoor_api.repository.WindowNodeRepository;
import com.atablood.iWindoor_api.repository.WindowUnitRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import com.atablood.iWindoor_api.service.DesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Eski düğüm tabanlı editör (mevcut mobil uygulama için korunuyor).
 * Her işlem önce düğümün hangi pencereye, o pencerenin hangi şirkete ait olduğunu kontrol eder.
 */
@Service
@RequiredArgsConstructor
public class DesignServiceImpl implements DesignService {

    private final WindowNodeRepository windowNodeRepository;
    private final WindowUnitRepository windowUnitRepository;
    private final ProfileRepository profileRepository;
    private final GlassRepository glassRepository;
    private final AccessGuard guard;

    @Override
    @Transactional
    public WindowNode splitNode(Long nodeId, boolean isVertical, User user) {
        WindowNode node = loadAuthorized(nodeId, user);
        boolean splittable = node.getNodeType() == NodeType.EMPTY || node.getNodeType() == NodeType.GLASS
                || (node.getNodeType() == NodeType.FRAME && node.getChildren().isEmpty());
        if (!splittable) throw new BadRequestException("NOT_SPLITTABLE", "Bu parça bölünemez.");

        node.setNodeType(isVertical ? NodeType.MULLION_VERTICAL : NodeType.MULLION_HORIZONTAL);
        node.setProfile(null);
        node.setGlass(null);
        node.setStoredPrice(null);

        double newWidth = isVertical ? node.getWidth() / 2 : node.getWidth();
        double newHeight = isVertical ? node.getHeight() : node.getHeight() / 2;
        createChildNode(node, newWidth, newHeight, 0);
        createChildNode(node, newWidth, newHeight, 1);
        return windowNodeRepository.save(node);
    }

    private void createChildNode(WindowNode parent, double w, double h, int order) {
        WindowNode child = new WindowNode();
        child.setNodeType(NodeType.EMPTY);
        child.setWidth(w);
        child.setHeight(h);
        child.setItemOrder(order);
        parent.addChild(child);
    }

    @Override
    @Transactional
    public WindowNode updateNodeType(Long nodeId, String nodeTypeStr, User user) {
        WindowNode node = loadAuthorized(nodeId, user);
        NodeType newType;
        try {
            newType = NodeType.valueOf(nodeTypeStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("INVALID_NODE_TYPE", "Geçersiz parça tipi: " + nodeTypeStr);
        }
        node.setNodeType(newType);
        return windowNodeRepository.save(node);
    }

    @Override
    @Transactional
    public WindowNode assignMaterial(Long nodeId, Long materialId, String materialType, User user) {
        WindowNode node = loadAuthorized(nodeId, user);
        if ("PROFILE".equalsIgnoreCase(materialType)) {
            Profile profile = profileRepository.findById(materialId).orElseThrow(() -> new NotFoundException("Profil", materialId));
            guard.checkCatalogRead(profile.getCompanyId(), user);
            node.setProfile(profile);
            node.setGlass(null);
            node.setStoredPrice(profile.getPricePerMeter()); // fiyatı sabitle
        } else if ("GLASS".equalsIgnoreCase(materialType)) {
            Glass glass = glassRepository.findById(materialId).orElseThrow(() -> new NotFoundException("Cam", materialId));
            guard.checkCatalogRead(glass.getCompanyId(), user);
            node.setGlass(glass);
            node.setProfile(null);
            node.setStoredPrice(glass.getPricePerSquareMeter()); // fiyatı sabitle
        } else {
            throw new BadRequestException("INVALID_MATERIAL_TYPE", "type PROFILE veya GLASS olmalı.");
        }
        return windowNodeRepository.save(node);
    }

    private WindowNode loadAuthorized(Long nodeId, User user) {
        WindowNode node = windowNodeRepository.findById(nodeId).orElseThrow(() -> new NotFoundException("Parça", nodeId));
        WindowNode root = node;
        int guardDepth = 0;
        while (root.getParent() != null && guardDepth++ < 64) root = root.getParent();
        WindowUnit unit = windowUnitRepository.findByRootNode(root)
                .orElseThrow(() -> new NotFoundException("Parçanın penceresi", nodeId));
        guard.checkProject(unit.getProject(), user);
        return node;
    }
}
