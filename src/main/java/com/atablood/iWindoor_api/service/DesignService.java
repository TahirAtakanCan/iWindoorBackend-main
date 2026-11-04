package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.entity.WindowNode;

/** ESKİ düğüm tabanlı editör. Yeni istemciler /api/v1/windows (tasarım belgesi) kullanır. */
public interface DesignService {
    WindowNode splitNode(Long nodeId, boolean isVertical, User user);

    WindowNode updateNodeType(Long nodeId, String nodeType, User user);

    WindowNode assignMaterial(Long nodeId, Long materialId, String materialType, User user);
}
