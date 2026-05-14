SET @add_material_assets_task_active_time_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_material_assets'
      AND index_name = 'idx_material_assets_task_active_time'
  ),
  "ALTER TABLE `biz_material_assets` ADD INDEX `idx_material_assets_task_active_time` (`task_id`, `is_deleted`, `create_time`)",
  "SELECT 1"
);
PREPARE add_material_assets_task_active_time_index_stmt FROM @add_material_assets_task_active_time_index_sql;
EXECUTE add_material_assets_task_active_time_index_stmt;
DEALLOCATE PREPARE add_material_assets_task_active_time_index_stmt;

SET @add_material_assets_owner_active_time_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_material_assets'
      AND index_name = 'idx_material_assets_owner_active_time'
  ),
  "ALTER TABLE `biz_material_assets` ADD INDEX `idx_material_assets_owner_active_time` (`owner_user_id`, `is_deleted`, `create_time` DESC)",
  "SELECT 1"
);
PREPARE add_material_assets_owner_active_time_index_stmt FROM @add_material_assets_owner_active_time_index_sql;
EXECUTE add_material_assets_owner_active_time_index_stmt;
DEALLOCATE PREPARE add_material_assets_owner_active_time_index_stmt;

SET @add_task_status_history_task_active_time_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_task_status_history'
      AND index_name = 'idx_task_status_history_task_active_time'
  ),
  "ALTER TABLE `biz_task_status_history` ADD INDEX `idx_task_status_history_task_active_time` (`task_id`, `is_deleted`, `change_time` DESC)",
  "SELECT 1"
);
PREPARE add_task_status_history_task_active_time_index_stmt FROM @add_task_status_history_task_active_time_index_sql;
EXECUTE add_task_status_history_task_active_time_index_stmt;
DEALLOCATE PREPARE add_task_status_history_task_active_time_index_stmt;

SET @add_task_stage_runs_task_active_started_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_task_stage_runs'
      AND index_name = 'idx_task_stage_runs_task_active_started'
  ),
  "ALTER TABLE `biz_task_stage_runs` ADD INDEX `idx_task_stage_runs_task_active_started` (`task_id`, `is_deleted`, `started_at` DESC)",
  "SELECT 1"
);
PREPARE add_task_stage_runs_task_active_started_index_stmt FROM @add_task_stage_runs_task_active_started_index_sql;
EXECUTE add_task_stage_runs_task_active_started_index_stmt;
DEALLOCATE PREPARE add_task_stage_runs_task_active_started_index_stmt;

SET @add_task_model_calls_task_active_started_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_task_model_calls'
      AND index_name = 'idx_task_model_calls_task_active_started'
  ),
  "ALTER TABLE `biz_task_model_calls` ADD INDEX `idx_task_model_calls_task_active_started` (`task_id`, `is_deleted`, `started_at`)",
  "SELECT 1"
);
PREPARE add_task_model_calls_task_active_started_index_stmt FROM @add_task_model_calls_task_active_started_index_sql;
EXECUTE add_task_model_calls_task_active_started_index_stmt;
DEALLOCATE PREPARE add_task_model_calls_task_active_started_index_stmt;

SET @add_task_attempts_queue_active_order_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_task_attempts'
      AND index_name = 'idx_task_attempts_queue_active_order'
  ),
  "ALTER TABLE `biz_task_attempts` ADD INDEX `idx_task_attempts_queue_active_order` (`status`, `is_deleted`, `queue_entered_at`, `attempt_no`, `create_time`, `task_id`)",
  "SELECT 1"
);
PREPARE add_task_attempts_queue_active_order_index_stmt FROM @add_task_attempts_queue_active_order_index_sql;
EXECUTE add_task_attempts_queue_active_order_index_stmt;
DEALLOCATE PREPARE add_task_attempts_queue_active_order_index_stmt;

SET @add_task_attempts_status_active_claimed_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_task_attempts'
      AND index_name = 'idx_task_attempts_status_active_claimed'
  ),
  "ALTER TABLE `biz_task_attempts` ADD INDEX `idx_task_attempts_status_active_claimed` (`status`, `is_deleted`, `claimed_at`, `update_time`, `task_id`, `worker_instance_id`)",
  "SELECT 1"
);
PREPARE add_task_attempts_status_active_claimed_index_stmt FROM @add_task_attempts_status_active_claimed_index_sql;
EXECUTE add_task_attempts_status_active_claimed_index_stmt;
DEALLOCATE PREPARE add_task_attempts_status_active_claimed_index_stmt;

SET @add_biz_tasks_active_create_time_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_tasks'
      AND index_name = 'idx_biz_tasks_active_create_time'
  ),
  "ALTER TABLE `biz_tasks` ADD INDEX `idx_biz_tasks_active_create_time` (`is_deleted`, `create_time` DESC)",
  "SELECT 1"
);
PREPARE add_biz_tasks_active_create_time_index_stmt FROM @add_biz_tasks_active_create_time_index_sql;
EXECUTE add_biz_tasks_active_create_time_index_stmt;
DEALLOCATE PREPARE add_biz_tasks_active_create_time_index_stmt;

SET @add_biz_tasks_active_status_owner_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_tasks'
      AND index_name = 'idx_biz_tasks_active_status_owner'
  ),
  "ALTER TABLE `biz_tasks` ADD INDEX `idx_biz_tasks_active_status_owner` (`is_deleted`, `status`, `owner_user_id`, `task_id`)",
  "SELECT 1"
);
PREPARE add_biz_tasks_active_status_owner_index_stmt FROM @add_biz_tasks_active_status_owner_index_sql;
EXECUTE add_biz_tasks_active_status_owner_index_stmt;
DEALLOCATE PREPARE add_biz_tasks_active_status_owner_index_stmt;

SET @add_worker_instances_active_heartbeat_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_worker_instances'
      AND index_name = 'idx_worker_instances_active_heartbeat'
  ),
  "ALTER TABLE `biz_worker_instances` ADD INDEX `idx_worker_instances_active_heartbeat` (`is_deleted`, `last_heartbeat_at` DESC)",
  "SELECT 1"
);
PREPARE add_worker_instances_active_heartbeat_index_stmt FROM @add_worker_instances_active_heartbeat_index_sql;
EXECUTE add_worker_instances_active_heartbeat_index_stmt;
DEALLOCATE PREPARE add_worker_instances_active_heartbeat_index_stmt;

SET @add_worker_instances_active_status_heartbeat_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_worker_instances'
      AND index_name = 'idx_worker_instances_active_status_heartbeat'
  ),
  "ALTER TABLE `biz_worker_instances` ADD INDEX `idx_worker_instances_active_status_heartbeat` (`is_deleted`, `status`, `last_heartbeat_at`)",
  "SELECT 1"
);
PREPARE add_worker_instances_active_status_heartbeat_index_stmt FROM @add_worker_instances_active_status_heartbeat_index_sql;
EXECUTE add_worker_instances_active_status_heartbeat_index_stmt;
DEALLOCATE PREPARE add_worker_instances_active_status_heartbeat_index_stmt;

SET @add_task_queue_events_active_time_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_task_queue_events'
      AND index_name = 'idx_task_queue_events_active_time'
  ),
  "ALTER TABLE `biz_task_queue_events` ADD INDEX `idx_task_queue_events_active_time` (`is_deleted`, `event_time` DESC)",
  "SELECT 1"
);
PREPARE add_task_queue_events_active_time_index_stmt FROM @add_task_queue_events_active_time_index_sql;
EXECUTE add_task_queue_events_active_time_index_stmt;
DEALLOCATE PREPARE add_task_queue_events_active_time_index_stmt;

SET @add_task_queue_events_task_active_time_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_task_queue_events'
      AND index_name = 'idx_task_queue_events_task_active_time'
  ),
  "ALTER TABLE `biz_task_queue_events` ADD INDEX `idx_task_queue_events_task_active_time` (`task_id`, `is_deleted`, `event_time` DESC)",
  "SELECT 1"
);
PREPARE add_task_queue_events_task_active_time_index_stmt FROM @add_task_queue_events_task_active_time_index_sql;
EXECUTE add_task_queue_events_task_active_time_index_stmt;
DEALLOCATE PREPARE add_task_queue_events_task_active_time_index_stmt;

SET @add_stage_workflows_owner_active_update_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_stage_workflows'
      AND index_name = 'idx_stage_workflows_owner_active_update'
  ),
  "ALTER TABLE `biz_stage_workflows` ADD INDEX `idx_stage_workflows_owner_active_update` (`owner_user_id`, `is_deleted`, `update_time` DESC)",
  "SELECT 1"
);
PREPARE add_stage_workflows_owner_active_update_index_stmt FROM @add_stage_workflows_owner_active_update_index_sql;
EXECUTE add_stage_workflows_owner_active_update_index_stmt;
DEALLOCATE PREPARE add_stage_workflows_owner_active_update_index_stmt;

SET @add_stage_versions_material_active_time_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_stage_versions'
      AND index_name = 'idx_stage_versions_material_active_time'
  ),
  "ALTER TABLE `biz_stage_versions` ADD INDEX `idx_stage_versions_material_active_time` (`material_asset_id`, `is_deleted`, `create_time` DESC)",
  "SELECT 1"
);
PREPARE add_stage_versions_material_active_time_index_stmt FROM @add_stage_versions_material_active_time_index_sql;
EXECUTE add_stage_versions_material_active_time_index_stmt;
DEALLOCATE PREPARE add_stage_versions_material_active_time_index_stmt;

SET @add_sys_user_role_status_created_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND index_name = 'idx_sys_user_role_status_created'
  ),
  "ALTER TABLE `sys_user` ADD INDEX `idx_sys_user_role_status_created` (`role`, `status`, `created_at` DESC)",
  "SELECT 1"
);
PREPARE add_sys_user_role_status_created_index_stmt FROM @add_sys_user_role_status_created_index_sql;
EXECUTE add_sys_user_role_status_created_index_stmt;
DEALLOCATE PREPARE add_sys_user_role_status_created_index_stmt;

SET @add_sys_user_created_at_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND index_name = 'idx_sys_user_created_at'
  ),
  "ALTER TABLE `sys_user` ADD INDEX `idx_sys_user_created_at` (`created_at` DESC)",
  "SELECT 1"
);
PREPARE add_sys_user_created_at_index_stmt FROM @add_sys_user_created_at_index_sql;
EXECUTE add_sys_user_created_at_index_stmt;
DEALLOCATE PREPARE add_sys_user_created_at_index_stmt;

SET @add_sys_invite_code_created_at_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_invite_code'
      AND index_name = 'idx_sys_invite_code_created_at'
  ),
  "ALTER TABLE `sys_invite_code` ADD INDEX `idx_sys_invite_code_created_at` (`created_at` DESC)",
  "SELECT 1"
);
PREPARE add_sys_invite_code_created_at_index_stmt FROM @add_sys_invite_code_created_at_index_sql;
EXECUTE add_sys_invite_code_created_at_index_stmt;
DEALLOCATE PREPARE add_sys_invite_code_created_at_index_stmt;

SET @add_sys_credit_transaction_user_type_time_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_credit_transaction'
      AND index_name = 'idx_sys_credit_transaction_user_type_time'
  ),
  "ALTER TABLE `sys_credit_transaction` ADD INDEX `idx_sys_credit_transaction_user_type_time` (`user_id`, `transaction_type`, `created_at`)",
  "SELECT 1"
);
PREPARE add_sys_credit_transaction_user_type_time_index_stmt FROM @add_sys_credit_transaction_user_type_time_index_sql;
EXECUTE add_sys_credit_transaction_user_type_time_index_stmt;
DEALLOCATE PREPARE add_sys_credit_transaction_user_type_time_index_stmt;

SET @add_request_logs_owner_active_time_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_request_logs'
      AND index_name = 'idx_request_logs_owner_active_time'
  ),
  "ALTER TABLE `biz_request_logs` ADD INDEX `idx_request_logs_owner_active_time` (`owner_user_id`, `is_deleted`, `started_at` DESC)",
  "SELECT 1"
);
PREPARE add_request_logs_owner_active_time_index_stmt FROM @add_request_logs_owner_active_time_index_sql;
EXECUTE add_request_logs_owner_active_time_index_stmt;
DEALLOCATE PREPARE add_request_logs_owner_active_time_index_stmt;

SET @add_request_logs_task_active_time_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_request_logs'
      AND index_name = 'idx_request_logs_task_active_time'
  ),
  "ALTER TABLE `biz_request_logs` ADD INDEX `idx_request_logs_task_active_time` (`task_id`, `is_deleted`, `started_at` DESC)",
  "SELECT 1"
);
PREPARE add_request_logs_task_active_time_index_stmt FROM @add_request_logs_task_active_time_index_sql;
EXECUTE add_request_logs_task_active_time_index_stmt;
DEALLOCATE PREPARE add_request_logs_task_active_time_index_stmt;

SET @add_request_logs_workflow_active_time_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_request_logs'
      AND index_name = 'idx_request_logs_workflow_active_time'
  ),
  "ALTER TABLE `biz_request_logs` ADD INDEX `idx_request_logs_workflow_active_time` (`workflow_id`, `is_deleted`, `started_at` DESC)",
  "SELECT 1"
);
PREPARE add_request_logs_workflow_active_time_index_stmt FROM @add_request_logs_workflow_active_time_index_sql;
EXECUTE add_request_logs_workflow_active_time_index_stmt;
DEALLOCATE PREPARE add_request_logs_workflow_active_time_index_stmt;

SET @add_request_logs_type_status_active_time_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_request_logs'
      AND index_name = 'idx_request_logs_type_status_active_time'
  ),
  "ALTER TABLE `biz_request_logs` ADD INDEX `idx_request_logs_type_status_active_time` (`request_type`, `status`, `is_deleted`, `started_at` DESC)",
  "SELECT 1"
);
PREPARE add_request_logs_type_status_active_time_index_stmt FROM @add_request_logs_type_status_active_time_index_sql;
EXECUTE add_request_logs_type_status_active_time_index_stmt;
DEALLOCATE PREPARE add_request_logs_type_status_active_time_index_stmt;
