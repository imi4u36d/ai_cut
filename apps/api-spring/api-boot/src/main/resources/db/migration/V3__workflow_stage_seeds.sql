SET @keyframe_seed_sql = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_stage_workflows'
      AND column_name = 'keyframe_seed'
  ),
  'SELECT 1',
  "ALTER TABLE `biz_stage_workflows` ADD COLUMN `keyframe_seed` int DEFAULT NULL COMMENT '关键帧阶段种子' AFTER `video_size`"
);
PREPARE keyframe_seed_stmt FROM @keyframe_seed_sql;
EXECUTE keyframe_seed_stmt;
DEALLOCATE PREPARE keyframe_seed_stmt;

SET @video_seed_sql = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_stage_workflows'
      AND column_name = 'video_seed'
  ),
  'SELECT 1',
  "ALTER TABLE `biz_stage_workflows` ADD COLUMN `video_seed` int DEFAULT NULL COMMENT '视频阶段种子' AFTER `keyframe_seed`"
);
PREPARE video_seed_stmt FROM @video_seed_sql;
EXECUTE video_seed_stmt;
DEALLOCATE PREPARE video_seed_stmt;
