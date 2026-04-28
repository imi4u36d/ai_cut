package com.jiandou.api.workflow.web;

import com.jiandou.api.config.ApiPathConstants;
import com.jiandou.api.workflow.application.WorkflowApplicationService;
import com.jiandou.api.workflow.web.dto.CreateMaterialGenerationRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPathConstants.MATERIAL_CENTER)
public class MaterialCenterController {

    private final WorkflowApplicationService workflowService;

    public MaterialCenterController(WorkflowApplicationService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/generations")
    public Map<String, Object> createMaterialGeneration(@RequestBody CreateMaterialGenerationRequest request) {
        return workflowService.createMaterialGeneration(request);
    }
}
