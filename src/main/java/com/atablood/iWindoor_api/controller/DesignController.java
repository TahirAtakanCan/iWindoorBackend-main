package com.atablood.iWindoor_api.controller;

import com.atablood.iWindoor_api.entity.WindowNode;
import com.atablood.iWindoor_api.service.DesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/design")
@RequiredArgsConstructor
public class DesignController {

    private final DesignService designService;

    // POST /api/v1/design/split/1?isVertical=true
    @PostMapping("/split/{nodeId}")
    public ResponseEntity<WindowNode> splitNode(@PathVariable Long nodeId,
                                                @RequestParam boolean isVertical) {
        return ResponseEntity.ok(designService.splitNode(nodeId, isVertical));
    }

    // POST /api/v1/design/update-type/5?type=GLASS
    @PostMapping("/update-type/{nodeId}")
    public ResponseEntity<WindowNode> updateNodeType(@PathVariable Long nodeId,
                                                     @RequestParam String type) {
        return ResponseEntity.ok(designService.updateNodeType(nodeId, type));
    }

    // POST /api/v1/design/assign-material/5?materialId=2&type=PROFILE
    @PostMapping("/assign-material/{nodeId}")
    public ResponseEntity<WindowNode> assignMaterial(@PathVariable Long nodeId,
                                                     @RequestParam Long materialId,
                                                     @RequestParam String type) { // type: "PROFILE" veya "GLASS"
        return ResponseEntity.ok(designService.assignMaterial(nodeId, materialId, type));
    }
}
