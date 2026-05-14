CREATE TABLE IF NOT EXISTS `sys_credit_account` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `balance` int NOT NULL DEFAULT '0' COMMENT '当前积分余额',
  `total_consumed` int NOT NULL DEFAULT '0' COMMENT '累计实际消耗积分',
  `total_adjusted` int NOT NULL DEFAULT '0' COMMENT '累计管理员调整积分',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_credit_account_user_id` (`user_id`),
  KEY `idx_sys_credit_account_balance` (`balance`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户积分账户表';

CREATE TABLE IF NOT EXISTS `sys_credit_rule` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `feature_code` varchar(64) NOT NULL COMMENT '功能编码',
  `display_name` varchar(128) NOT NULL DEFAULT '' COMMENT '功能名称',
  `cost` int NOT NULL DEFAULT '0' COMMENT '单次消耗积分',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_credit_rule_feature_code` (`feature_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='功能积分消耗规则表';

CREATE TABLE IF NOT EXISTS `sys_credit_transaction` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `transaction_id` varchar(64) NOT NULL COMMENT '积分流水业务ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `feature_code` varchar(64) NOT NULL DEFAULT '' COMMENT '功能编码',
  `transaction_type` varchar(32) NOT NULL COMMENT '流水类型',
  `amount_delta` int NOT NULL DEFAULT '0' COMMENT '积分变动值，扣减为负数',
  `balance_before` int NOT NULL DEFAULT '0' COMMENT '变动前余额',
  `balance_after` int NOT NULL DEFAULT '0' COMMENT '变动后余额',
  `related_run_id` varchar(64) NOT NULL DEFAULT '' COMMENT '关联生成运行ID',
  `related_task_id` varchar(64) NOT NULL DEFAULT '' COMMENT '关联任务ID',
  `related_workflow_id` varchar(64) NOT NULL DEFAULT '' COMMENT '关联工作流ID',
  `reason` varchar(512) NOT NULL DEFAULT '' COMMENT '变动原因',
  `metadata_json` json NOT NULL COMMENT '扩展元数据',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_credit_transaction_id` (`transaction_id`),
  KEY `idx_sys_credit_transaction_user_time` (`user_id`, `created_at`),
  KEY `idx_sys_credit_transaction_feature_time` (`feature_code`, `created_at`),
  KEY `idx_sys_credit_transaction_run_id` (`related_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户积分流水表';

INSERT INTO `sys_credit_rule` (`feature_code`, `display_name`, `cost`)
VALUES
  ('IMAGE_GENERATION', '图片生成', 10),
  ('VIDEO_GENERATION', '视频生成', 50)
ON DUPLICATE KEY UPDATE `feature_code` = VALUES(`feature_code`);

INSERT INTO `sys_credit_account` (`user_id`, `balance`, `total_consumed`, `total_adjusted`)
SELECT `id`, 50, 0, 0
FROM `sys_user`
WHERE LOWER(`username`) <> 'admin'
ON DUPLICATE KEY UPDATE `user_id` = VALUES(`user_id`);
