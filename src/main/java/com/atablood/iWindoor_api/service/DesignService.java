package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.entity.WindowNode;

public interface DesignService {
    // Bir parçayı (Node) dikey veya yatay böler
    WindowNode splitNode(Long nodeId, boolean isVertical);

    WindowNode updateNodeType(Long nodeId, String nodeType);

    WindowNode assignMaterial(Long nodeId, Long materialId, String materialType);
}
