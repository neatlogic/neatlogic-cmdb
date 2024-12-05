ALTER TABLE `cmdb_sync_audit`
    ADD COLUMN `current_data_id` varchar(255) NULL COMMENT '当前数据id，用于恢复执行时用' AFTER `error`;
ALTER TABLE `cmdb_sync_audit`
    ADD COLUMN `config` text NULL COMMENT '额外配置' AFTER `current_data_id`;
ALTER TABLE `cmdb_sync_audit`
    MODIFY COLUMN `status` enum('doing','done','pausing','paused') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '状态' AFTER `input_from`;