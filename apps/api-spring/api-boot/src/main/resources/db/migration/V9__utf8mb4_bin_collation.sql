ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'biz_tasks' AND table_type = 'BASE TABLE'),
  'ALTER TABLE `biz_tasks` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'biz_task_status_history' AND table_type = 'BASE TABLE'),
  'ALTER TABLE `biz_task_status_history` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'biz_task_attempts' AND table_type = 'BASE TABLE'),
  'ALTER TABLE `biz_task_attempts` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'biz_task_stage_runs' AND table_type = 'BASE TABLE'),
  'ALTER TABLE `biz_task_stage_runs` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'biz_task_queue_events' AND table_type = 'BASE TABLE'),
  'ALTER TABLE `biz_task_queue_events` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'biz_worker_instances' AND table_type = 'BASE TABLE'),
  'ALTER TABLE `biz_worker_instances` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'biz_task_model_calls' AND table_type = 'BASE TABLE'),
  'ALTER TABLE `biz_task_model_calls` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'biz_task_results' AND table_type = 'BASE TABLE'),
  'ALTER TABLE `biz_task_results` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'biz_material_assets' AND table_type = 'BASE TABLE'),
  'ALTER TABLE `biz_material_assets` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'biz_stage_workflows' AND table_type = 'BASE TABLE'),
  'ALTER TABLE `biz_stage_workflows` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'biz_stage_versions' AND table_type = 'BASE TABLE'),
  'ALTER TABLE `biz_stage_versions` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND table_type = 'BASE TABLE'),
  'ALTER TABLE `sys_user` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_user_model_credential' AND table_type = 'BASE TABLE'),
  'ALTER TABLE `sys_user_model_credential` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_invite_code' AND table_type = 'BASE TABLE'),
  'ALTER TABLE `sys_invite_code` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
