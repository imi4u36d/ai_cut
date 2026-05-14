package com.jiandou.api.media;

import com.jiandou.api.task.infrastructure.mybatis.MaterialAssetEntity;
import com.jiandou.api.workflow.application.WorkflowRepository;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 历史素材缩略图回填服务。
 */
@Component
public class MaterialAssetThumbnailBackfillService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MaterialAssetThumbnailBackfillService.class);
    private static final int BATCH_SIZE = 50;
    private static final int MAX_BATCHES = 200;

    private final WorkflowRepository workflowRepository;
    private final MaterialAssetThumbnailService thumbnailService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public MaterialAssetThumbnailBackfillService(
        WorkflowRepository workflowRepository,
        MaterialAssetThumbnailService thumbnailService
    ) {
        this.workflowRepository = workflowRepository;
        this.thumbnailService = thumbnailService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        long startedAt = System.nanoTime();
        int scanned = 0;
        int updated = 0;
        int failed = 0;
        long afterId = 0L;
        try {
            for (int batch = 0; batch < MAX_BATCHES; batch++) {
                List<MaterialAssetEntity> assets = workflowRepository.listMaterialAssetsMissingThumbnailsAfterId(afterId, BATCH_SIZE);
                if (assets.isEmpty()) {
                    break;
                }
                scanned += assets.size();
                for (MaterialAssetEntity asset : assets) {
                    afterId = Math.max(afterId, asset.getId() == null ? afterId : asset.getId());
                    try {
                        String thumbnailUrl = thumbnailService.ensureThumbnail(asset);
                        if (!thumbnailUrl.isBlank()) {
                            workflowRepository.updateMaterialAssetThumbnail(asset.getMaterialAssetId(), thumbnailUrl);
                            updated++;
                        } else {
                            failed++;
                        }
                    } catch (RuntimeException ex) {
                        failed++;
                        log.warn("material thumbnail backfill failed: assetId={}", asset.getMaterialAssetId(), ex);
                    }
                }
            }
        } finally {
            running.set(false);
        }
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
        log.info("material thumbnail backfill finished: scanned={}, updated={}, failed={}, elapsedMillis={}", scanned, updated, failed, elapsedMillis);
    }
}
