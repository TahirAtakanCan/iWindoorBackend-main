package com.atablood.iWindoor_api.service.impl;

import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.repository.GlassRepository;
import com.atablood.iWindoor_api.repository.ProfileRepository;
import com.atablood.iWindoor_api.repository.WindowNodeRepository;
import com.atablood.iWindoor_api.service.DesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DesignServiceImpl implements DesignService {

    private final WindowNodeRepository windowNodeRepository;
    private final ProfileRepository profileRepository;
    private final GlassRepository glassRepository;

    @Override
    @Transactional
    public WindowNode splitNode(Long nodeId, boolean isVertical) {
        WindowNode node = windowNodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Parça bulunamadı"));

        node.setNodeType(isVertical ? NodeType.MULLION_VERTICAL : NodeType.MULLION_HORIZONTAL);

        // Bölünen parça artık bir "Kayıt" olduğu için eski Kasa/Kanat profilini temizle
        // Kullanıcı daha sonra buna "Kayıt Profili" atayacak.
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
        child.setParent(parent);
        parent.getChildren().add(child);
    }

    @Override
    @Transactional
    public WindowNode updateNodeType(Long nodeId, String nodeTypeStr) {
        WindowNode node = windowNodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Parça bulunamadı"));
        try {
            NodeType newType = NodeType.valueOf(nodeTypeStr);
            // Kasa ve Kayıtlar değiştirilemez, sadece iç parçalar
            if (node.getNodeType() == NodeType.FRAME || node.getNodeType().name().startsWith("MULLION")) {
                // throw new RuntimeException("Taşıyıcı parçalar değiştirilemez!");
                // Esneklik için bu kontrolü gevşetebiliriz veya UI'da yönetebiliriz.
            }
            node.setNodeType(newType);
            return windowNodeRepository.save(node);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Geçersiz tip");
        }
    }

    @Override
    @Transactional
    public WindowNode assignMaterial(Long nodeId, Long materialId, String materialType) {
        WindowNode node = windowNodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Parça bulunamadı"));

        if ("PROFILE".equalsIgnoreCase(materialType)) {
            Profile profile = profileRepository.findById(materialId)
                    .orElseThrow(() -> new RuntimeException("Profil bulunamadı"));
            node.setProfile(profile);
            node.setGlass(null);
            // --- FİYATI SABİTLE ---
            node.setStoredPrice(profile.getPricePerMeter());

        } else if ("GLASS".equalsIgnoreCase(materialType)) {
            Glass glass = glassRepository.findById(materialId)
                    .orElseThrow(() -> new RuntimeException("Cam bulunamadı"));
            node.setGlass(glass);
            node.setProfile(null);
            // --- FİYATI SABİTLE ---
            node.setStoredPrice(glass.getPricePerSquareMeter());
        }
        return windowNodeRepository.save(node);
    }
}