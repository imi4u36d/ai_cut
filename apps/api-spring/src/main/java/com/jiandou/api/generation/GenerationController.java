package com.jiandou.api.generation;

import com.jiandou.api.generation.application.GenerationApplicationService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

/**
 * 生成控制器。
 */
@RestController
@RequestMapping("/api/v2/generation")
public class GenerationController {

    private final GenerationApplicationService generationService;

    /**
     * 创建新的生成控制器。
     * @param generationService 生成服务值
     */
    public GenerationController(GenerationApplicationService generationService) {
        this.generationService = generationService;
    }

    /**
     * 处理目录。
     * @return 处理结果
     */
    @GetMapping("/catalog")
    public Map<String, Object> catalog() {
        return generationService.catalog();
    }

    /**
     * 创建运行。
     * @param request 请求体
     * @return 处理结果
     */
    @PostMapping("/runs")
    public Map<String, Object> createRun(@RequestBody Map<String, Object> request) {
        try {
            return generationService.createRun(request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (UnsupportedGenerationKindException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (GenerationConfigurationException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        } catch (GenerationProviderException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, ex.getMessage(), ex);
        } catch (GenerationNotImplementedException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, ex.getMessage(), ex);
        }
    }

    /**
     * 列出Runs。
     * @return 处理结果
     */
    @GetMapping("/runs")
    public List<Map<String, Object>> listRuns() {
        return generationService.listRuns(100);
    }

    /**
     * 返回运行。
     * @param runId 运行标识值
     * @return 处理结果
     */
    @GetMapping("/runs/{runId}")
    public Map<String, Object> getRun(@PathVariable String runId) {
        try {
            return generationService.getRun(runId);
        } catch (GenerationRunNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "generation run not found", ex);
        }
    }

    /**
     * 处理usage。
     * @return 处理结果
     */
    @GetMapping("/usage")
    public Map<String, Object> usage() {
        return generationService.usage();
    }
}
