SET @drop_stage_versions_old_order_index_sql = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_stage_versions'
      AND index_name = 'idx_stage_versions_workflow_stage_clip'
  ),
  "ALTER TABLE `biz_stage_versions` DROP INDEX `idx_stage_versions_workflow_stage_clip`",
  "SELECT 1"
);
PREPARE drop_stage_versions_old_order_index_stmt FROM @drop_stage_versions_old_order_index_sql;
EXECUTE drop_stage_versions_old_order_index_stmt;
DEALLOCATE PREPARE drop_stage_versions_old_order_index_stmt;

SET @add_stage_versions_active_order_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_stage_versions'
      AND index_name = 'idx_stage_versions_workflow_active_order'
  ),
  "ALTER TABLE `biz_stage_versions` ADD INDEX `idx_stage_versions_workflow_active_order` (`workflow_id`, `is_deleted`, `stage_type`, `clip_index`, `version_no` DESC)",
  "SELECT 1"
);
PREPARE add_stage_versions_active_order_index_stmt FROM @add_stage_versions_active_order_index_sql;
EXECUTE add_stage_versions_active_order_index_stmt;
DEALLOCATE PREPARE add_stage_versions_active_order_index_stmt;
