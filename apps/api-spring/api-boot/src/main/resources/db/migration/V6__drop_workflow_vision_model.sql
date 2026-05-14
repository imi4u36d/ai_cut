SET @drop_workflow_vision_model_sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_stage_workflows'
      AND column_name = 'vision_model'
  ),
  "ALTER TABLE `biz_stage_workflows` DROP COLUMN `vision_model`",
  "SELECT 1"
);
PREPARE drop_workflow_vision_model_stmt FROM @drop_workflow_vision_model_sql;
EXECUTE drop_workflow_vision_model_stmt;
DEALLOCATE PREPARE drop_workflow_vision_model_stmt;
