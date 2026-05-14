SET @add_material_assets_thumbnail_url_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_material_assets'
      AND column_name = 'thumbnail_url'
  ),
  "ALTER TABLE `biz_material_assets` ADD COLUMN `thumbnail_url` varchar(2048) NOT NULL DEFAULT '' COMMENT '列表缩略图 URL' AFTER `public_url`",
  "SELECT 1"
);
PREPARE add_material_assets_thumbnail_url_stmt FROM @add_material_assets_thumbnail_url_sql;
EXECUTE add_material_assets_thumbnail_url_stmt;
DEALLOCATE PREPARE add_material_assets_thumbnail_url_stmt;

SET @add_material_assets_thumbnail_active_index_sql = IF(
  NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_material_assets'
      AND index_name = 'idx_material_assets_thumbnail_active'
  ),
  "ALTER TABLE `biz_material_assets` ADD INDEX `idx_material_assets_thumbnail_active` (`is_deleted`, `media_type`, `id`)",
  "SELECT 1"
);
PREPARE add_material_assets_thumbnail_active_index_stmt FROM @add_material_assets_thumbnail_active_index_sql;
EXECUTE add_material_assets_thumbnail_active_index_stmt;
DEALLOCATE PREPARE add_material_assets_thumbnail_active_index_stmt;
