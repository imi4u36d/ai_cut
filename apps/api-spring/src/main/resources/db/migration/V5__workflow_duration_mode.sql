SET @duration_mode_sql = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_stage_workflows'
      AND column_name = 'duration_mode'
  ),
  'SELECT 1',
  "ALTER TABLE `biz_stage_workflows` ADD COLUMN `duration_mode` varchar(16) NOT NULL DEFAULT 'auto' COMMENT '镜头时长模式 auto/manual' AFTER `video_seed`"
);
PREPARE duration_mode_stmt FROM @duration_mode_sql;
EXECUTE duration_mode_stmt;
DEALLOCATE PREPARE duration_mode_stmt;
