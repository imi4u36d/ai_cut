package com.jiandou.api.workflow.web;

import com.jiandou.api.config.ApiPathConstants;
import com.jiandou.api.workflow.application.WorkflowApplicationService;
import com.jiandou.api.workflow.web.dto.AdjustStoryboardRequest;
import com.jiandou.api.workflow.web.dto.CreateWorkflowRequest;
import com.jiandou.api.workflow.web.dto.RateStageVersionRequest;
import com.jiandou.api.workflow.web.dto.RateWorkflowRequest;
import com.jiandou.api.workflow.web.dto.SelectCharacterSheetAssetRequest;
import com.jiandou.api.workflow.web.dto.UpdateWorkflowSettingsRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPathConstants.WORKFLOWS)
public class WorkflowController {

    private final WorkflowApplicationService workflowService;

    public WorkflowController(WorkflowApplicationService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    public Map<String, Object> createWorkflow(@RequestBody CreateWorkflowRequest request) {
        return workflowService.createWorkflow(request);
    }

    @GetMapping
    public List<Map<String, Object>> listWorkflows() {
        return workflowService.listWorkflows();
    }

    @GetMapping("/{workflowId}")
    public Map<String, Object> getWorkflow(@PathVariable String workflowId) {
        return workflowService.getWorkflow(workflowId);
    }

    @DeleteMapping("/{workflowId}")
    public Map<String, Object> deleteWorkflow(@PathVariable String workflowId) {
        return workflowService.deleteWorkflow(workflowId);
    }

    @PatchMapping("/{workflowId}/settings")
    public Map<String, Object> updateWorkflowSettings(
        @PathVariable String workflowId,
        @RequestBody UpdateWorkflowSettingsRequest request
    ) {
        return workflowService.updateWorkflowSettings(workflowId, request);
    }

    @PostMapping("/{workflowId}/storyboards/generate")
    public Map<String, Object> generateStoryboard(@PathVariable String workflowId) {
        return workflowService.generateStoryboard(workflowId);
    }

    @PostMapping("/{workflowId}/storyboards/{versionId}/select")
    public Map<String, Object> selectStoryboard(@PathVariable String workflowId, @PathVariable String versionId) {
        return workflowService.selectStoryboard(workflowId, versionId);
    }

    @PostMapping("/{workflowId}/storyboards/{versionId}/adjust")
    public Map<String, Object> adjustStoryboard(
        @PathVariable String workflowId,
        @PathVariable String versionId,
        @RequestBody(required = false) AdjustStoryboardRequest request
    ) {
        return workflowService.adjustStoryboard(workflowId, versionId, request == null ? "" : request.prompt());
    }

    @PostMapping("/{workflowId}/clips/{clipIndex}/keyframes/generate")
    public Map<String, Object> generateKeyframe(
        @PathVariable String workflowId,
        @PathVariable int clipIndex
    ) {
        return workflowService.generateKeyframe(workflowId, clipIndex);
    }

    @PostMapping("/{workflowId}/clips/{clipIndex}/keyframes/{frameRole}/generate")
    public Map<String, Object> generateKeyframeFrame(
        @PathVariable String workflowId,
        @PathVariable int clipIndex,
        @PathVariable String frameRole
    ) {
        return workflowService.generateKeyframeFrame(workflowId, clipIndex, frameRole);
    }

    @PostMapping("/{workflowId}/clips/{clipIndex}/keyframes/{versionId}/select")
    public Map<String, Object> selectKeyframe(@PathVariable String workflowId, @PathVariable int clipIndex, @PathVariable String versionId) {
        return workflowService.selectKeyframe(workflowId, clipIndex, versionId);
    }

    @PostMapping("/{workflowId}/clips/{clipIndex}/keyframes/{versionId}/frames/{frameRole}/select")
    public Map<String, Object> selectKeyframeFrame(
        @PathVariable String workflowId,
        @PathVariable int clipIndex,
        @PathVariable String versionId,
        @PathVariable String frameRole
    ) {
        return workflowService.selectKeyframeFrame(workflowId, clipIndex, versionId, frameRole);
    }

    @PostMapping("/{workflowId}/character-sheets/{clipIndex}/select-asset")
    public Map<String, Object> selectCharacterSheetAsset(
        @PathVariable String workflowId,
        @PathVariable int clipIndex,
        @RequestBody SelectCharacterSheetAssetRequest request
    ) {
        return workflowService.selectCharacterSheetAsset(workflowId, clipIndex, request);
    }

    @PostMapping("/{workflowId}/clips/{clipIndex}/videos/generate")
    public Map<String, Object> generateVideo(
        @PathVariable String workflowId,
        @PathVariable int clipIndex
    ) {
        return workflowService.generateVideo(workflowId, clipIndex);
    }

    @PostMapping("/{workflowId}/clips/{clipIndex}/videos/{versionId}/select")
    public Map<String, Object> selectVideo(@PathVariable String workflowId, @PathVariable int clipIndex, @PathVariable String versionId) {
        return workflowService.selectVideo(workflowId, clipIndex, versionId);
    }

    @PostMapping("/{workflowId}/finalize")
    public Map<String, Object> finalizeWorkflow(@PathVariable String workflowId) {
        return workflowService.finalizeWorkflow(workflowId);
    }

    @PostMapping("/{workflowId}/rating")
    public Map<String, Object> rateWorkflow(@PathVariable String workflowId, @RequestBody RateWorkflowRequest request) {
        return workflowService.rateWorkflow(workflowId, request);
    }

    @PatchMapping("/{workflowId}/versions/{versionId}/rating")
    public Map<String, Object> rateStageVersion(
        @PathVariable String workflowId,
        @PathVariable String versionId,
        @RequestBody RateStageVersionRequest request
    ) {
        return workflowService.rateStageVersion(workflowId, versionId, request);
    }

    @DeleteMapping("/{workflowId}/versions/{versionId}")
    public Map<String, Object> deleteStageVersion(@PathVariable String workflowId, @PathVariable String versionId) {
        return workflowService.deleteStageVersion(workflowId, versionId);
    }
}
