SET @task_concurrency_limit_sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'sys_user'
        AND column_name = 'task_concurrency_limit'
    ),
    'SELECT 1',
    'ALTER TABLE `sys_user` ADD COLUMN `task_concurrency_limit` int unsigned NOT NULL DEFAULT ''1'' COMMENT ''任务并发额度'' AFTER `status`'
  )
);
PREPARE stmt FROM @task_concurrency_limit_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
