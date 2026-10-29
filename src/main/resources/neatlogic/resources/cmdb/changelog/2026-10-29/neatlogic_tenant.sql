ALTER TABLE `cmdb_sync_ci_collection` ADD COLUMN `default_condition` text DEFAULT NULL COMMENT '手动及接口执行的预设过滤条件，压缩JSON';
